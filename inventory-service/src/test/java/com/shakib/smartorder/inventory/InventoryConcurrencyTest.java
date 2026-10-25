package com.shakib.smartorder.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.shakib.smartorder.inventory.dto.CreateInventoryRequest;
import com.shakib.smartorder.inventory.dto.ReserveLine;
import com.shakib.smartorder.inventory.exception.InsufficientStockException;
import com.shakib.smartorder.inventory.service.InventoryService;

/**
 * Needs Docker running. Starts a throwaway MySQL, so it never touches your dev database.
 */
@SpringBootTest(properties = { "messaging.enabled=false",
        "app.jwt.secret=test-only-secret-that-is-at-least-32-characters-long" })
@Testcontainers
class InventoryConcurrencyTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired
    InventoryService service;

    @Test
    void twentyBuyersForTenUnitsNeverOversell() throws Exception {
        final long productId = 1001L;
        final int stock = 10;
        final int buyers = 20;

        service.create(new CreateInventoryRequest(productId, stock));

        ExecutorService pool = Executors.newFixedThreadPool(buyers);
        CountDownLatch ready = new CountDownLatch(buyers);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger reserved = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < buyers; i++) {
            final long orderId = 5000L + i;
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    service.reserve(orderId, List.of(new ReserveLine(productId, 1)));
                    reserved.incrementAndGet();
                } catch (InsufficientStockException e) {
                    rejected.incrementAndGet();
                }
                return null;
            }));
        }

        ready.await();
        go.countDown();
        for (Future<Void> f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(stock, reserved.get(), "exactly the available stock must be reserved");
        assertEquals(buyers - stock, rejected.get(), "everyone else must be rejected");
        assertEquals(0, service.get(productId).availableQuantity(), "stock must end at zero, never negative");
    }

    @Test
    void releaseReturnsStockOnlyOnce() {
        final long productId = 2002L;
        final long orderId = 7000L;

        service.create(new CreateInventoryRequest(productId, 5));
        service.reserve(orderId, List.of(new ReserveLine(productId, 3)));
        assertEquals(2, service.get(productId).availableQuantity());

        service.release(orderId);
        service.release(orderId);

        assertEquals(5, service.get(productId).availableQuantity(), "a second release must not add stock again");
    }

    @Test
    void reserveTwiceForSameOrderTakesStockOnce() {
        final long productId = 3003L;
        final long orderId = 8000L;

        service.create(new CreateInventoryRequest(productId, 5));
        service.reserve(orderId, List.of(new ReserveLine(productId, 2)));
        service.reserve(orderId, List.of(new ReserveLine(productId, 2)));

        assertEquals(3, service.get(productId).availableQuantity(), "a retried reserve must not take stock twice");
    }
}

