package com.shakib.smartorder.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/*
  A relative change: +5 adds five units, -3 removes three.
  A relative change is safe under concurrency; "set quantity to X" is not.
 */
public record AdjustStockRequest(
        @NotNull(message = "delta is required")
        @Min(value = -1000000, message = "delta must be at least -1,000,000")
        @Max(value = 1000000, message = "delta must be at most 1,000,000")
        Integer delta) {
}

