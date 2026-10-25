package com.shakib.smartorder.order.dto;

import java.util.List;

import com.shakib.smartorder.order.enums.OrderType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * All items must belong to ONE brand (use cart checkout to order from several brands at once).
 * type: PURCHASE (default) or RESERVATION. A reservation holds the stock for holdHours (default 24,
 * at most 72); the customer confirms it before it expires, or the stock is released.
 */
public record CreateOrderRequest(
        @NotEmpty(message = "items must not be empty")
        @Size(max = 50, message = "an order can have at most 50 lines")
        List<@Valid OrderItemRequest> items,

        @NotNull(message = "shippingAddress is required")
        @Valid
        ShippingAddressRequest shippingAddress,

        OrderType type,

        @Min(value = 1, message = "holdHours must be at least 1")
        @Max(value = 72, message = "holdHours must be at most 72")
        Integer holdHours) {
}
