package com.shakib.smartorder.order.billing;

import java.math.BigDecimal;

//The priced result for one line. lineTotal = (listPrice - discountPerUnit) x quantity, GST included.
public record PricedLine(
        Long productId,
        int quantity,
        BigDecimal listPrice,
        BigDecimal discountPerUnit,
        BigDecimal discountAmount,
        String offerTitle,
        BigDecimal lineTotal,
        BigDecimal taxableValue,
        BigDecimal gstRatePercent,
        BigDecimal cgst,
        BigDecimal sgst,
        BigDecimal igst) {
}
