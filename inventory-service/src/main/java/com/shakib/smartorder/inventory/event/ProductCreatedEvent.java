package com.shakib.smartorder.inventory.event;

import java.time.Instant;

// Received from the Product service when a seller adds a product. Same shape as the event Product publishes.
public record ProductCreatedEvent(
        String eventId,
        Long productId,
        Long sellerId,
        int initialStock,
        String correlationId,
        Instant occurredAt) {
}
