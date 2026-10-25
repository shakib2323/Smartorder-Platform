package com.shakib.smartorder.product.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.shakib.smartorder.product.enums.DiscountType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OfferRequest(
        @NotBlank(message = "title is required")
        @Size(max = 120, message = "title must be at most 120 characters")
        String title,

        // Leave empty to apply the offer to every product of your brand.
        Long productId,

        @NotNull(message = "discountType is required")
        DiscountType discountType,

        @NotNull(message = "discountValue is required")
        @DecimalMin(value = "0.01", message = "discountValue must be positive")
        @Digits(integer = 8, fraction = 2, message = "discountValue allows up to 8 digits and 2 decimals")
        BigDecimal discountValue,

        @Min(value = 1, message = "minQuantity must be at least 1")
        @Max(value = 1000, message = "minQuantity must be at most 1000")
        Integer minQuantity,

        @NotNull(message = "startsAt is required")
        Instant startsAt,

        @NotNull(message = "endsAt is required")
        Instant endsAt) {
}
