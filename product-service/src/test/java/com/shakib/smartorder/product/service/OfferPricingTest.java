package com.shakib.smartorder.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.shakib.smartorder.product.entity.Offer;
import com.shakib.smartorder.product.enums.DiscountType;

class OfferPricingTest {

    private static final BigDecimal PRICE = new BigDecimal("1000.00");

    private static Offer offer(String title, DiscountType type, String value, int minQty, Long productId) {
        Instant now = Instant.now();
        return new Offer(1L, productId, title, type, new BigDecimal(value), minQty, now.minus(1, ChronoUnit.HOURS),
                now.plus(1, ChronoUnit.HOURS));
    }

    @Test
    void percentOfferTakesThatShareOffTheUnitPrice() {
        Offer tenPercent = offer("10% off", DiscountType.PERCENT, "10", 1, null);

        assertEquals(new BigDecimal("100.00"), tenPercent.discountPerUnit(PRICE));
    }

    @Test
    void flatOfferNeverExceedsThePrice() {
        Offer huge = offer("Flat 5000", DiscountType.FLAT, "5000", 1, null);

        assertEquals(new BigDecimal("1000.00"), huge.discountPerUnit(PRICE));
    }

    @Test
    void theBiggestSavingWins_offersDoNotStack() {
        Offer percent = offer("10% off", DiscountType.PERCENT, "10", 1, null);
        Offer flat = offer("Flat 150", DiscountType.FLAT, "150", 1, null);

        assertEquals("Flat 150", OfferPricing.bestOffer(5L, PRICE, 1, List.of(percent, flat)).getTitle());
    }

    @Test
    void anOfferNeedsEnoughQuantity() {
        Offer buyThree = offer("Buy 3", DiscountType.PERCENT, "20", 3, null);

        assertNull(OfferPricing.bestOffer(5L, PRICE, 2, List.of(buyThree)));
        assertEquals("Buy 3", OfferPricing.bestOffer(5L, PRICE, 3, List.of(buyThree)).getTitle());
    }

    @Test
    void aProductOfferOnlyCoversThatProduct() {
        Offer forProduct9 = offer("Only 9", DiscountType.PERCENT, "50", 1, 9L);

        assertNull(OfferPricing.bestOffer(5L, PRICE, 1, List.of(forProduct9)));
        assertEquals("Only 9", OfferPricing.bestOffer(9L, PRICE, 1, List.of(forProduct9)).getTitle());
    }

    @Test
    void aBrandWideOfferCoversEveryProduct() {
        Offer brandWide = offer("Everything 5%", DiscountType.PERCENT, "5", 1, null);

        assertEquals("Everything 5%", OfferPricing.bestOffer(123L, PRICE, 1, List.of(brandWide)).getTitle());
    }

    @Test
    void percentRoundsHalfUpToTwoDecimals() {
        // 12.5% of 99.99 = 12.49875 -> 12.50
        Offer offer = offer("12.5%", DiscountType.PERCENT, "12.5", 1, null);

        assertEquals(new BigDecimal("12.50"), offer.discountPerUnit(new BigDecimal("99.99")));
    }
}
