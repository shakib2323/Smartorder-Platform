package com.shakib.smartorder.order.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Flat delivery fee that is waived once the items total reaches {@code freeAbove}.
public record ShippingPolicy(BigDecimal flatFee, BigDecimal freeAbove) {

    public BigDecimal feeFor(BigDecimal itemsTotal) {
        if (flatFee.signum() <= 0 || itemsTotal.compareTo(freeAbove) >= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return flatFee.setScale(2, RoundingMode.HALF_UP);
    }
}
