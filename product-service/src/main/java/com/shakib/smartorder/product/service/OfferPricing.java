package com.shakib.smartorder.product.service;

import java.math.BigDecimal;
import java.util.Collection;

import com.shakib.smartorder.product.entity.Offer;

/*
  Picks the offer that saves the customer the most. Offers never stack: the best single one wins.
  (The Order service applies the same rule when it prices an order, using the real quantity.)
 */
public final class OfferPricing {

    private OfferPricing() {
    }

    // @return the best applicable offer, or null when none applies 
    public static Offer bestOffer(Long productId, BigDecimal unitPrice, int quantity, Collection<Offer> offers) 
    {
        Offer best = null;
        BigDecimal bestDiscount = BigDecimal.ZERO;
        for (Offer offer : offers) {
            if (!offer.appliesTo(productId, quantity))
            {
                continue;
            }
            BigDecimal discount = offer.discountPerUnit(unitPrice);
            if (discount.compareTo(bestDiscount) > 0) {
                best = offer;
                bestDiscount = discount;
            }
        }
        return best;
    }
}
