package com.shakib.smartorder.order.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(List<CartLineResponse> items, int totalItems, BigDecimal totalAmount,
        BigDecimal totalSavings) {

    public CartResponse {
        items = List.copyOf(items);
    }
}
