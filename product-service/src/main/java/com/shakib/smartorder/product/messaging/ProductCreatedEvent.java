package com.shakib.smartorder.product.messaging;

import java.time.Instant;

/*
  Tells Inventory a product exists, who owns it (sellerId) and how much stock to start with.
  Inventory creates the stock row and remembers the owner, so it can check seller permissions
  without calling back into this service.
 */
public record ProductCreatedEvent(
        String eventId,
        Long productId,
        Long sellerId,
        int initialStock,
        String correlationId,
        Instant occurredAt) {
}
