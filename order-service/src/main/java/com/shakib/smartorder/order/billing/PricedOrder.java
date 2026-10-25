package com.shakib.smartorder.order.billing;

import java.math.BigDecimal;
import java.util.List;

/*
  subtotal      = list price x quantity, before any offer
  discountTotal = what the seller's offers took off
  itemsTotal    = subtotal - discountTotal (GST included)
  taxableTotal / cgstTotal / sgstTotal / igstTotal = the GST split of itemsTotal
  grandTotal    = itemsTotal + shippingFee, the amount the customer pays
 */
public record PricedOrder(
        List<PricedLine> lines,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal itemsTotal,
        BigDecimal taxableTotal,
        BigDecimal cgstTotal,
        BigDecimal sgstTotal,
        BigDecimal igstTotal,
        BigDecimal shippingFee,
        BigDecimal grandTotal) {
}
