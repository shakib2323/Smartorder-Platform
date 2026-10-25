package com.shakib.smartorder.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateInventoryRequest(
        @NotNull(message = "productId is required")
        @Positive(message = "productId must be positive")
        Long productId,

        @NotNull(message = "availableQuantity is required")
        @Min(value = 0, message = "availableQuantity must not be negative")
        @Max(value = 1000000, message = "availableQuantity must be at most 1,000,000")
        Integer availableQuantity) {
}

