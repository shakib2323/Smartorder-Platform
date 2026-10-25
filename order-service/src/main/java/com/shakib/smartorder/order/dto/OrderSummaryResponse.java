package com.shakib.smartorder.order.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.shakib.smartorder.order.entity.Order;
import com.shakib.smartorder.order.enums.OrderStatus;
import com.shakib.smartorder.order.enums.OrderType;

/** List view: no line items, so listing never triggers one extra query per order. */
public record OrderSummaryResponse(
        Long id,
        OrderStatus status,
        OrderType orderType,
        String brandName,
        BigDecimal totalAmount,
        String currency,
        Instant createdAt) {

    public static OrderSummaryResponse from(Order o) {
        return new OrderSummaryResponse(o.getId(), o.getStatus(), o.getOrderType(),
                o.getSeller() == null ? null : o.getSeller().getBrandName(), o.getTotalAmount(), o.getCurrency(),
                o.getCreatedAt());
    }
}
