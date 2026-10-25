package com.shakib.smartorder.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutResponse(List<OrderResponse> orders, BigDecimal grandTotal) {

    public CheckoutResponse {
        orders = List.copyOf(orders);
    }
}
