package com.shakib.smartorder.order.dto;

/** replayed = true when the same Idempotency-Key was already used and the original order is returned. */
public record PlacementResult(OrderResponse order, boolean replayed) {
}
