package com.shakib.smartorder.order.events;

import java.time.Instant;

// Received from the Inventory service: the order could not be fulfilled.
public record InventoryRejectedEvent(
        String eventId,
        Long orderId,
        String reason,
        String correlationId,
        Instant occurredAt) {
}
