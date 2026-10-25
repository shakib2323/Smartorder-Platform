package com.shakib.smartorder.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.shakib.smartorder.order.entity.Order;
import com.shakib.smartorder.order.entity.OrderTotals;
import com.shakib.smartorder.order.enums.OrderStatus;
import com.shakib.smartorder.order.enums.OrderType;

/**
 * totalAmount is what the customer pays = (subtotal - discountTotal) + shippingFee.
 * Prices include GST; taxableTotal, cgstTotal, sgstTotal and igstTotal show the split of that GST.
 */
public record OrderResponse(
        Long id,
        OrderStatus status,
        OrderType orderType,
        Instant reserveExpiresAt,
        Long brandId,
        String brandName,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal taxableTotal,
        BigDecimal cgstTotal,
        BigDecimal sgstTotal,
        BigDecimal igstTotal,
        BigDecimal shippingFee,
        BigDecimal totalAmount,
        String currency,
        String paymentMethod,
        String trackingNumber,
        Instant createdAt,
        Instant shippedAt,
        Instant deliveredAt,
        List<OrderItemResponse> items,
        ShippingAddressResponse shippingAddress) {

    public static OrderResponse from(Order o) {
        List<OrderItemResponse> items = o.getItems().stream().map(OrderItemResponse::from).toList();
        OrderTotals t = o.getTotals();
        return new OrderResponse(o.getId(), o.getStatus(), o.getOrderType(), o.getReserveExpiresAt(), o.getBrandId(),
                o.getSeller() == null ? null : o.getSeller().getBrandName(),
                t.getSubtotal(), t.getDiscountTotal(), t.getTaxableTotal(), t.getCgstTotal(), t.getSgstTotal(),
                t.getIgstTotal(), t.getShippingFee(), o.getTotalAmount(), o.getCurrency(), o.getPaymentMethod(),
                o.getTrackingNumber(), o.getCreatedAt(), o.getShippedAt(), o.getDeliveredAt(), items,
                ShippingAddressResponse.from(o.getShippingAddress()));
    }
}
