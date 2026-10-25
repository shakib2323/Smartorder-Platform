package com.shakib.smartorder.inventory.event;

import java.time.Instant;

public record OrderCancelledEvent(
        String eventId,
        Long orderId,
        String correlationId,
        Instant occurredAt) {
}
