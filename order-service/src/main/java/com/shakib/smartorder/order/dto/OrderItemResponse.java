package com.shakib.smartorder.order.dto;

import java.math.BigDecimal;

import com.shakib.smartorder.order.entity.OrderItem;

public record OrderItemResponse(
        Long productId,
        String productName,
        String sku,
        String size,
        String color,
        int quantity,
        BigDecimal unitPrice,
        String offerTitle,
        BigDecimal discountAmount,
        BigDecimal gstRatePercent,
        BigDecimal lineTotal) {

    public static OrderItemResponse from(OrderItem i) {
        return new OrderItemResponse(i.getProductId(), i.getProductName(), i.getSku(), i.getSize(), i.getColor(),
                i.getQuantity(), i.getUnitPrice(), i.getOfferTitle(), i.getDiscountAmount(), i.getGstRatePercent(),
                i.getLineTotal());
    }
}
