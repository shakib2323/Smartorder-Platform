package com.shakib.smartorder.order.cart.dto;

import java.math.BigDecimal;

public record CartLineResponse(
        Long productId,
        String name,
        Long brandId,
        String brandName,
        BigDecimal unitPrice,
        BigDecimal discountPerUnit,
        String offerTitle,
        int quantity,
        BigDecimal lineTotal,
        boolean available) {
}
