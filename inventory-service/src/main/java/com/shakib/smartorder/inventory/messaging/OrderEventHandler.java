package com.shakib.smartorder.inventory.messaging;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shakib.smartorder.inventory.dto.ReserveLine;
import com.shakib.smartorder.inventory.entity.OutboxEvent;
import com.shakib.smartorder.inventory.event.InventoryRejectedEvent;
import com.shakib.smartorder.inventory.event.InventoryReservedEvent;
import com.shakib.smartorder.inventory.event.OrderCancelledEvent;
import com.shakib.smartorder.inventory.event.OrderCreatedEvent;
import com.shakib.smartorder.inventory.exception.InsufficientStockException;
import com.shakib.smartorder.inventory.exception.InventoryNotFoundException;
import com.shakib.smartorder.inventory.repository.OutboxEventRepository;
import com.shakib.smartorder.inventory.service.InventoryService;

/*
  Applies Order events to inventory. Kept separate from the Kafka listener so it can be
  tested with only a database.
 
  Every event is handled exactly once in effect (Kafka delivers at least once):
  the "processed" mark, the stock change and the reply event are written in ONE transaction.
 
  Rejection needs care: a failed reservation rolls its transaction back, and that would also
  erase the "processed" mark. So the rejection is recorded in a second, fresh transaction.
 */
@Component
public class OrderEventHandler {

    public static final String ORDER_CREATED = "OrderCreated";
    public static final String ORDER_CANCELLED = "OrderCancelled";
    public static final String INVENTORY_RESERVED = "InventoryReserved";
    public static final String INVENTORY_REJECTED = "InventoryRejected";

    private static final Logger log = LoggerFactory.getLogger(OrderEventHandler.class);

    private final InventoryService inventoryService;
    private final ProcessedEventStore processedEvents;
    private final OutboxEventRepository outboxRepository;
    private final TransactionTemplate tx;
    private final ObjectMapper objectMapper;

    public OrderEventHandler(InventoryService inventoryService, ProcessedEventStore processedEvents,
            OutboxEventRepository outboxRepository, TransactionTemplate tx, ObjectMapper objectMapper) 
    {
        this.inventoryService = inventoryService;
        this.processedEvents = processedEvents;
        this.outboxRepository = outboxRepository;
        this.tx = tx;
        this.objectMapper = objectMapper;
    }

    public void handle(String eventId, String eventType, String payload) 
    {
        switch (eventType) {
            case ORDER_CREATED -> onOrderCreated(eventId, read(payload, OrderCreatedEvent.class));
            case ORDER_CANCELLED -> onOrderCancelled(eventId, read(payload, OrderCancelledEvent.class));
            default -> log.warn("Ignoring unknown event type {} (eventId={})", eventType, eventId);
        }
    }

    private void onOrderCreated(String eventId, OrderCreatedEvent event) 
    {
        List<ReserveLine> lines = event.items().stream()
                .map(item -> new ReserveLine(item.productId(), item.quantity()))
                .toList();
        try {
            tx.executeWithoutResult(status -> {
                if (!processedEvents.markProcessed(eventId)) {
                    log.info("Event {} already processed, skipping", eventId);
                    return;
                }
                inventoryService.reserve(event.orderId(), lines);
                String replyId = UUID.randomUUID().toString();
                saveOutbox(replyId, event.orderId(), INVENTORY_RESERVED, event.correlationId(),
                        new InventoryReservedEvent(replyId, event.orderId(), event.correlationId(), Instant.now()));
                log.info("Stock reserved for order {}", event.orderId());
            });
        } catch (InsufficientStockException | InventoryNotFoundException e)
        {
            tx.executeWithoutResult(status -> {
                if (!processedEvents.markProcessed(eventId)) {
                    return;
                }
                String replyId = UUID.randomUUID().toString();
                saveOutbox(replyId, event.orderId(), INVENTORY_REJECTED, event.correlationId(),
                        new InventoryRejectedEvent(replyId, event.orderId(), e.getMessage(),
                                event.correlationId(), Instant.now()));
                log.info("Order {} rejected: {}", event.orderId(), e.getMessage());
            });
        }
    }

    private void onOrderCancelled(String eventId, OrderCancelledEvent event) 
    {
        tx.executeWithoutResult(status -> {
            if (!processedEvents.markProcessed(eventId)) {
                log.info("Event {} already processed, skipping", eventId);
                return;
            }
            boolean released = inventoryService.releaseIfPresent(event.orderId());
            log.info("Order {} cancelled, stock released={}", event.orderId(), released);
        });
    }

    private void saveOutbox(String eventId, Long orderId, String eventType, String correlationId, Object event) 
    {
        outboxRepository.save(new OutboxEvent(eventId, orderId, eventType, Topics.INVENTORY_EVENTS,
                toJson(event), correlationId));
    }

    private <T> T read(String payload, Class<T> type)
    {
        try {
            return objectMapper.readValue(payload, type);
        } catch (JsonProcessingException e) {
            // Not retryable: the same bad message will never become valid. It goes to the dead-letter topic.
            throw new IllegalArgumentException("Malformed " + type.getSimpleName() + " payload", e);
        }
    }

    private String toJson(Object event) 
    {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize event", e);
        }
    }
}
