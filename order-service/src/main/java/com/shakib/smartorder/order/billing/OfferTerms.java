package com.shakib.smartorder.order.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/*
  A seller offer as the pricing engine needs it. productId null = covers every product of the brand.
  Same rules as the Product service's Offer, so the price a shopper sees is the price they pay.
 */
public record OfferTerms(String title, Type type, BigDecimal value, int minQuantity, Long productId) 
{

    public enum Type { 
    	PERCENT, FLAT 
    }

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public boolean appliesTo(Long forProductId, int quantity) 
    {
        boolean productMatches = productId == null || productId.equals(forProductId);
        return productMatches && quantity >= Math.max(minQuantity, 1);
    }

    // Discount on ONE unit, never more than the unit price. 
    public BigDecimal discountPerUnit(BigDecimal unitPrice) 
    {
        BigDecimal raw = type == Type.PERCENT
                ? unitPrice.multiply(value).divide(HUNDRED, 2, RoundingMode.HALF_UP)
                : value;
        return raw.min(unitPrice).setScale(2, RoundingMode.HALF_UP);
    }
}
