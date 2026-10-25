package com.shakib.smartorder.inventory.event;

import java.time.Instant;
import java.util.List;

// Received from the Order service. Same shape as the event Order publishes.
public record OrderCreatedEvent(
        String eventId,
        Long orderId,
        List<Item> items,
        String correlationId,
        Instant occurredAt) {

    public record Item(Long productId, int quantity) {
    }
}
