package com.shakib.smartorder.order.events;

import java.time.Instant;

// Received from the Inventory service: stock was reserved for the order.
public record InventoryReservedEvent(
        String eventId,
        Long orderId,
        String correlationId,
        Instant occurredAt) {
}
