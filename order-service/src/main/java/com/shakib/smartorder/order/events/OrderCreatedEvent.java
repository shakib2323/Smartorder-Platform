package com.shakib.smartorder.order.events;

import java.time.Instant;
import java.util.List;

// Contract for the "OrderCreated" event. Moves to the shared common-events module in the Kafka step. 
public record OrderCreatedEvent(
        String eventId,
        Long orderId,
        List<Item> items,
        String correlationId,
        Instant occurredAt) {

    public record Item(Long productId, int quantity) {
    }
}
