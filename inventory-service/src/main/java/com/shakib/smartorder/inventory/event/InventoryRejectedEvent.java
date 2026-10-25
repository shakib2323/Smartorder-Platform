package com.shakib.smartorder.inventory.event;

import java.time.Instant;

public record InventoryRejectedEvent(
        String eventId,
        Long orderId,
        String reason,
        String correlationId,
        Instant occurredAt) {
}
