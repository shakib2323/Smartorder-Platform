package com.shakib.smartorder.order.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.shakib.smartorder.order.enums.OrderStatus;
import com.shakib.smartorder.order.repository.OrderRepository;

/*
  Releases the stock of reservations that were not confirmed in time.
  Safe with several instances running: expiring is a conditional database update, so only one of
  them wins for each order.
 */
@Component
@ConditionalOnProperty(name = "messaging.enabled", havingValue = "true", matchIfMissing = true)
public class ReservationExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryJob.class);
    private static final int BATCH_SIZE = 100;

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public ReservationExpiryJob(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @Scheduled(fixedDelayString = "${reservation.sweep-interval-ms:60000}")
    public void sweep()
    {
        List<Long> expired = orderRepository.findExpiredReservationIds(OrderStatus.RESERVED, Instant.now(),
                PageRequest.of(0, BATCH_SIZE));
        for (Long orderId : expired) {
            try {
                orderService.expireReservation(orderId);
            } catch (RuntimeException e) {
                // One bad order must not stop the others; it is retried on the next run.
                log.warn("Could not expire reservation {}: {}", orderId, e.toString());
            }
        }
    }
}
