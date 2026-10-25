package com.shakib.smartorder.order.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shakib.smartorder.order.events.InventoryRejectedEvent;
import com.shakib.smartorder.order.events.InventoryReservedEvent;
import com.shakib.smartorder.order.service.OrderService;

/*
  Applies Inventory's answer to the order: PENDING -> CONFIRMED or REJECTED.
  The "processed" mark and the status change commit in one transaction, so a message
  delivered twice changes the order once. If the order is in a state that cannot take the
  answer, the exception rolls everything back and the error handler moves the message
  to the dead-letter topic (no point retrying a state conflict).
 */
@Component
public class InventoryEventHandler {

    public static final String INVENTORY_RESERVED = "InventoryReserved";
    public static final String INVENTORY_REJECTED = "InventoryRejected";

    private static final Logger log = LoggerFactory.getLogger(InventoryEventHandler.class);

    private final OrderService orderService;
    private final ProcessedEventStore processedEvents;
    private final TransactionTemplate tx;
    private final ObjectMapper objectMapper;

    public InventoryEventHandler(OrderService orderService, ProcessedEventStore processedEvents,
            TransactionTemplate tx, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.processedEvents = processedEvents;
        this.tx = tx;
        this.objectMapper = objectMapper;
    }

    public void handle(String eventId, String eventType, String payload) {
        switch (eventType) {
            case INVENTORY_RESERVED -> apply(eventId, read(payload, InventoryReservedEvent.class).orderId(), true);
            case INVENTORY_REJECTED -> apply(eventId, read(payload, InventoryRejectedEvent.class).orderId(), false);
            default -> log.warn("Ignoring unknown event type {} (eventId={})", eventType, eventId);
        }
    }

    private void apply(String eventId, Long orderId, boolean reserved) 
    {
        tx.executeWithoutResult(status -> {
            if (!processedEvents.markProcessed(eventId)) {
                log.info("Event {} already processed, skipping", eventId);
                return;
            }
            if (reserved) {
                orderService.markConfirmed(orderId);
            } else {
                orderService.markRejected(orderId);
            }
        });
    }

    private <T> T read(String payload, Class<T> type) 
    {
        try {
            return objectMapper.readValue(payload, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Malformed " + type.getSimpleName() + " payload", e);
        }
    }
}
