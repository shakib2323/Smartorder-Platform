package com.shakib.smartorder.order.events;

import java.time.Instant;

// Contract for the "OrderCancelled" event. Inventory reacts by releasing the reserved stock. 
public record OrderCancelledEvent(
        String eventId,
        Long orderId,
        String correlationId,
        Instant occurredAt) {
}
