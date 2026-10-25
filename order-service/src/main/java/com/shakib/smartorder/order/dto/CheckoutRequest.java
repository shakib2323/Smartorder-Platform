package com.shakib.smartorder.order.dto;

import com.shakib.smartorder.order.enums.OrderType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Checks out the whole cart. One order is created per brand, because each seller invoices and ships separately. */
public record CheckoutRequest(
        @NotNull(message = "shippingAddress is required")
        @Valid
        ShippingAddressRequest shippingAddress,

        OrderType type,

        @Min(value = 1, message = "holdHours must be at least 1")
        @Max(value = 72, message = "holdHours must be at most 72")
        Integer holdHours) {
}
