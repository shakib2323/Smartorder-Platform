package com.shakib.smartorder.inventory.event;

import java.time.Instant;

public record InventoryReservedEvent(
        String eventId,
        Long orderId,
        String correlationId,
        Instant occurredAt) {
}
