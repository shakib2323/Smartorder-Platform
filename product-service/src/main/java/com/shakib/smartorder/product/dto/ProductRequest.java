package com.shakib.smartorder.product.dto;

import java.math.BigDecimal;

import com.shakib.smartorder.product.enums.ProductCategory;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank(message = "name is required")
        @Size(max = 200, message = "name must be at most 200 characters")
        String name,

        @Size(max = 1000, message = "description must be at most 1000 characters")
        String description,

        // Selling price per unit, GST included.
        @NotNull(message = "price is required")
        @DecimalMin(value = "0.01", message = "price must be at least 0.01")
        @Digits(integer = 10, fraction = 2, message = "price allows up to 10 digits and 2 decimals")
        BigDecimal price,

        @NotNull(message = "category is required")
        ProductCategory category,

        // Clothing variants: create one product per size/colour (same name, different size/colour/sku).
        @Size(max = 20, message = "size must be at most 20 characters")
        String size,

        @Size(max = 30, message = "color must be at most 30 characters")
        String color,

        @Size(max = 50, message = "sku must be at most 50 characters")
        @Pattern(regexp = "^[A-Za-z0-9._-]*$", message = "sku may contain letters, digits, dot, dash and underscore")
        String sku,

        // One of 0, 5, 12, 18, 28. Empty means 0.
        @DecimalMin(value = "0", message = "gstRatePercent must not be negative")
        @DecimalMax(value = "28", message = "gstRatePercent must be at most 28")
        BigDecimal gstRatePercent,

        @Pattern(regexp = "^$|^[0-9]{4,8}$", message = "hsnCode must be 4 to 8 digits")
        String hsnCode,

        // Only used when creating: the first stock level. Inventory creates the stock row from it.
        @Min(value = 0, message = "initialStock must not be negative")
        @Max(value = 1000000, message = "initialStock must be at most 1,000,000")
        Integer initialStock) {
}
