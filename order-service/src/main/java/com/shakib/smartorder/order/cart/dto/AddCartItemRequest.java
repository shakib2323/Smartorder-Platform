package com.shakib.smartorder.order.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddCartItemRequest(
        @NotNull(message = "productId is required") Long productId,
        @NotNull(message = "quantity is required") @Min(value = 1, message = "quantity must be at least 1")
        @Max(value = 99, message = "quantity cannot exceed 99") Integer quantity) {
}
