package com.shakib.smartorder.order.dto;

import java.math.BigDecimal;
import java.util.Map;

import com.shakib.smartorder.order.enums.OrderStatus;

/** A seller's dashboard numbers. sales = total of orders that are CONFIRMED, SHIPPED or DELIVERED. */
public record SellerSummaryResponse(long totalOrders, Map<OrderStatus, Long> ordersByStatus, BigDecimal sales) {
}
