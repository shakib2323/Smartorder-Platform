package com.shakib.smartorder.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shakib.smartorder.inventory.dto.CreateInventoryRequest;
import com.shakib.smartorder.inventory.event.OrderCancelledEvent;
import com.shakib.smartorder.inventory.event.OrderCreatedEvent;
import com.shakib.smartorder.inventory.messaging.OrderEventHandler;
import com.shakib.smartorder.inventory.repository.OutboxEventRepository;
import com.shakib.smartorder.inventory.service.InventoryService;

/**
 * Needs Docker running (MySQL only, no Kafka). Tests what Inventory does with Order events.
 */
@SpringBootTest(properties = { "messaging.enabled=false",
        "app.jwt.secret=test-only-secret-that-is-at-least-32-characters-long" })
@Testcontainers
class OrderEventHandlerTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired
    OrderEventHandler handler;

    @Autowired
    InventoryService inventoryService;

    @Autowired
    OutboxEventRepository outbox;

    @Autowired
    ObjectMapper mapper;

    private static String newId() {
        return UUID.randomUUID().toString();
    }

    private String createdEvent(long orderId, long productId, int quantity) throws Exception {
        return mapper.writeValueAsString(new OrderCreatedEvent(newId(), orderId,
                List.of(new OrderCreatedEvent.Item(productId, quantity)), "corr-test", Instant.now()));
    }

    private String cancelledEvent(long orderId) throws Exception {
        return mapper.writeValueAsString(new OrderCancelledEvent(newId(), orderId, "corr-test", Instant.now()));
    }

    private int stock(long productId) {
        return inventoryService.get(productId).availableQuantity();
    }

    @Test
    void orderCreatedReservesStockAndEmitsInventoryReserved() throws Exception {
        inventoryService.create(new CreateInventoryRequest(11L, 10));

        handler.handle(newId(), "OrderCreated", createdEvent(101L, 11L, 3));

        assertEquals(7, stock(11L));
        assertEquals(1, outbox.countByAggregateIdAndEventType(101L, "InventoryReserved"));
        assertEquals(0, outbox.countByAggregateIdAndEventType(101L, "InventoryRejected"));
    }

    @Test
    void insufficientStockEmitsInventoryRejectedAndKeepsStockUnchanged() throws Exception {
        inventoryService.create(new CreateInventoryRequest(12L, 2));

        handler.handle(newId(), "OrderCreated", createdEvent(102L, 12L, 5));

        assertEquals(2, stock(12L));
        assertEquals(1, outbox.countByAggregateIdAndEventType(102L, "InventoryRejected"));
        assertEquals(0, outbox.countByAggregateIdAndEventType(102L, "InventoryReserved"));
    }

    @Test
    void unknownProductIsRejected() throws Exception {
        handler.handle(newId(), "OrderCreated", createdEvent(103L, 9999L, 1));

        assertEquals(1, outbox.countByAggregateIdAndEventType(103L, "InventoryRejected"));
    }

    @Test
    void sameEventDeliveredTwiceIsAppliedOnce() throws Exception {
        inventoryService.create(new CreateInventoryRequest(13L, 10));
        String eventId = newId();
        String payload = createdEvent(104L, 13L, 4);

        handler.handle(eventId, "OrderCreated", payload);
        handler.handle(eventId, "OrderCreated", payload);

        assertEquals(6, stock(13L), "stock must be taken only once");
        assertEquals(1, outbox.countByAggregateIdAndEventType(104L, "InventoryReserved"));
    }

    @Test
    void cancelReturnsStockOnlyOnce() throws Exception {
        inventoryService.create(new CreateInventoryRequest(14L, 10));
        handler.handle(newId(), "OrderCreated", createdEvent(105L, 14L, 4));
        assertEquals(6, stock(14L));

        String cancelId = newId();
        String cancelPayload = cancelledEvent(105L);
        handler.handle(cancelId, "OrderCancelled", cancelPayload);
        handler.handle(cancelId, "OrderCancelled", cancelPayload);
        handler.handle(newId(), "OrderCancelled", cancelledEvent(105L));

        assertEquals(10, stock(14L), "stock must be returned exactly once");
    }

    @Test
    void cancelForOrderWithoutReservationIsHarmless() throws Exception {
        inventoryService.create(new CreateInventoryRequest(15L, 5));

        handler.handle(newId(), "OrderCancelled", cancelledEvent(106L));

        assertEquals(5, stock(15L));
    }
}
