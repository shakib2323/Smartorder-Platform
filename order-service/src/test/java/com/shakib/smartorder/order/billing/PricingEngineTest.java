package com.shakib.smartorder.order.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class PricingEngineTest {

    private static final ShippingPolicy SHIPPING = new ShippingPolicy(bd("49.00"), bd("999.00"));

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static LineInput line(long productId, int qty, String price, String gst, OfferTerms... offers) {
        return new LineInput(productId, qty, bd(price), bd(gst), List.of(offers));
    }

    private static OfferTerms percent(String title, String value, int minQty) {
        return new OfferTerms(title, OfferTerms.Type.PERCENT, bd(value), minQty, null);
    }

    private static OfferTerms flat(String title, String value, int minQty) {
        return new OfferTerms(title, OfferTerms.Type.FLAT, bd(value), minQty, null);
    }

    @Test
    void splitsGstInsideTheSellingPrice_withinOneState() {
        PricedOrder order = PricingEngine.price(List.of(line(1, 1, "1180.00", "18")), true, SHIPPING);

        assertEquals(bd("1000.00"), order.taxableTotal());
        assertEquals(bd("90.00"), order.cgstTotal());
        assertEquals(bd("90.00"), order.sgstTotal());
        assertEquals(bd("0.00"), order.igstTotal());
        assertEquals(bd("1180.00"), order.grandTotal()); // 1180 is above the free-shipping limit
    }

    @Test
    void usesIgstBetweenStates() {
        PricedOrder order = PricingEngine.price(List.of(line(1, 1, "1180.00", "18")), false, SHIPPING);

        assertEquals(bd("180.00"), order.igstTotal());
        assertEquals(bd("0.00"), order.cgstTotal());
        assertEquals(bd("0.00"), order.sgstTotal());
    }

    @Test
    void appliesPercentOffer_andTracksTheSavings() {
        PricedOrder order = PricingEngine.price(
                List.of(line(1, 2, "1000.00", "5", percent("Diwali 10%", "10", 1))), true, SHIPPING);

        PricedLine priced = order.lines().get(0);
        assertEquals(bd("100.00"), priced.discountPerUnit());
        assertEquals(bd("200.00"), priced.discountAmount());
        assertEquals("Diwali 10%", priced.offerTitle());
        assertEquals(bd("1800.00"), priced.lineTotal());
        assertEquals(bd("2000.00"), order.subtotal());
        assertEquals(bd("200.00"), order.discountTotal());
        assertEquals(bd("1800.00"), order.itemsTotal());
    }

    @Test
    void quantityOfferNeedsEnoughUnits() {
        OfferTerms buyThree = percent("Buy 3 save 20%", "20", 3);

        PricedLine two = PricingEngine.priceLine(line(1, 2, "500.00", "0", buyThree), true);
        PricedLine three = PricingEngine.priceLine(line(1, 3, "500.00", "0", buyThree), true);

        assertNull(two.offerTitle());
        assertEquals(bd("1000.00"), two.lineTotal());
        assertEquals("Buy 3 save 20%", three.offerTitle());
        assertEquals(bd("1200.00"), three.lineTotal());
    }

    @Test
    void offersDoNotStack_theBiggestSavingWins() {
        // 10% of 1000 = 100, flat 150 is better.
        PricedLine priced = PricingEngine.priceLine(
                line(1, 1, "1000.00", "0", percent("10% off", "10", 1), flat("Flat 150", "150", 1)), true);

        assertEquals("Flat 150", priced.offerTitle());
        assertEquals(bd("850.00"), priced.lineTotal());
    }

    @Test
    void offerOfAnotherProductIsIgnored() {
        OfferTerms forProduct9 = new OfferTerms("Only product 9", OfferTerms.Type.PERCENT, bd("50"), 1, 9L);

        PricedLine priced = PricingEngine.priceLine(line(1, 1, "100.00", "0", forProduct9), true);

        assertNull(priced.offerTitle());
        assertEquals(bd("100.00"), priced.lineTotal());
    }

    @Test
    void flatDiscountNeverMakesAPriceNegative() {
        PricedLine priced = PricingEngine.priceLine(line(1, 1, "100.00", "18", flat("Huge", "5000", 1)), true);

        assertEquals(bd("100.00"), priced.discountPerUnit());
        assertEquals(bd("0.00"), priced.lineTotal());
        assertEquals(bd("0.00"), priced.taxableValue());
    }

    @Test
    void charges_shippingBelowTheLimit_andWaivesItAtTheLimit() {
        PricedOrder small = PricingEngine.price(List.of(line(1, 1, "500.00", "0")), true, SHIPPING);
        PricedOrder atLimit = PricingEngine.price(List.of(line(1, 1, "999.00", "0")), true, SHIPPING);

        assertEquals(bd("49.00"), small.shippingFee());
        assertEquals(bd("549.00"), small.grandTotal());
        assertEquals(bd("0.00"), atLimit.shippingFee());
        assertEquals(bd("999.00"), atLimit.grandTotal());
    }

    @Test
    void sumsSeveralLines() {
        PricedOrder order = PricingEngine.price(
                List.of(line(1, 1, "105.00", "5"), line(2, 2, "118.00", "18")), true, SHIPPING);

        // line 1: taxable 100.00, tax 5.00; line 2: total 236.00, taxable 200.00, tax 36.00
        assertEquals(bd("300.00"), order.taxableTotal());
        assertEquals(bd("41.00"), order.cgstTotal().add(order.sgstTotal()));
        assertEquals(bd("341.00"), order.itemsTotal());
        assertEquals(bd("390.00"), order.grandTotal()); // + 49.00 shipping
    }

    @Test
    void taxAlwaysAddsUpToTheLineTotal_forEveryPrice() {
        String[] rates = { "0", "5", "12", "18", "28" };
        for (String rate : rates) {
            for (int cents = 1; cents <= 20_000; cents++) {
                BigDecimal price = BigDecimal.valueOf(cents, 2);
                for (boolean intra : new boolean[] { true, false }) {
                    PricedLine l = PricingEngine.priceLine(line(1, 1, price.toPlainString(), rate), intra);
                    BigDecimal rebuilt = l.taxableValue().add(l.cgst()).add(l.sgst()).add(l.igst());
                    assertEquals(l.lineTotal(), rebuilt, "price " + price + " rate " + rate + " intra " + intra);
                    assertTrue(l.cgst().subtract(l.sgst()).abs().compareTo(bd("0.01")) <= 0
                            || !intra, "CGST and SGST differ by more than one paisa for " + price);
                }
            }
        }
    }
}
