package com.shakib.smartorder.order.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

///
//  Prices an order: applies the best offer per line, splits the GST that is already inside the
//  price, adds shipping. Pure arithmetic with BigDecimal, no framework, so it is easy to test.
// 
//  Prices are GST-inclusive (like an Indian MRP). For a line total T and rate r:
//    taxable value = T x 100 / (100 + r),  tax = T - taxable value.
//  Within one state the tax is split half CGST / half SGST; between states it is all IGST.
public final class PricingEngine {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWO = new BigDecimal("2");

    private PricingEngine() {
    }

    // Same state as the seller means CGST + SGST, otherwise IGST. Compared by name, ignoring case and spaces.
    public static boolean isIntraState(String sellerState, String buyerState) {
        return sellerState != null && buyerState != null && sellerState.trim().equalsIgnoreCase(buyerState.trim());
    }

    public static PricedOrder price(List<LineInput> inputs, boolean intraState, ShippingPolicy shipping) 
    {
        List<PricedLine> lines = new ArrayList<>();
        BigDecimal subtotal = zero();
        BigDecimal discountTotal = zero();
        BigDecimal itemsTotal = zero();
        BigDecimal taxable = zero();
        BigDecimal cgst = zero();
        BigDecimal sgst = zero();
        BigDecimal igst = zero();

        for (LineInput input : inputs) {
            PricedLine line = priceLine(input, intraState);
            lines.add(line);
            subtotal = subtotal.add(input.listPrice().multiply(BigDecimal.valueOf(input.quantity())));
            discountTotal = discountTotal.add(line.discountAmount());
            itemsTotal = itemsTotal.add(line.lineTotal());
            taxable = taxable.add(line.taxableValue());
            cgst = cgst.add(line.cgst());
            sgst = sgst.add(line.sgst());
            igst = igst.add(line.igst());
        }

        BigDecimal shippingFee = shipping.feeFor(itemsTotal);
        return new PricedOrder(List.copyOf(lines), money(subtotal), discountTotal, itemsTotal, taxable, cgst, sgst,
                igst, shippingFee, itemsTotal.add(shippingFee));
    }

    public static PricedLine priceLine(LineInput input, boolean intraState) 
    {
        BigDecimal qty = BigDecimal.valueOf(input.quantity());
        OfferTerms best = bestOffer(input);
        BigDecimal discountPerUnit = best == null ? zero() : best.discountPerUnit(input.listPrice());

        BigDecimal lineTotal = money(input.listPrice().subtract(discountPerUnit).multiply(qty));
        BigDecimal discountAmount = money(discountPerUnit.multiply(qty));

        BigDecimal rate = input.gstRatePercent() == null ? BigDecimal.ZERO : input.gstRatePercent();
        BigDecimal taxableValue = lineTotal.multiply(HUNDRED)
                .divide(HUNDRED.add(rate), 2, RoundingMode.HALF_UP);
        BigDecimal tax = lineTotal.subtract(taxableValue);

        BigDecimal cgst = zero();
        BigDecimal sgst = zero();
        BigDecimal igst = zero();
        if (intraState) {
            cgst = tax.divide(TWO, 2, RoundingMode.HALF_UP);
            sgst = tax.subtract(cgst); // the remainder, so cgst + sgst always equals the tax exactly
        } else {
            igst = tax;
        }

        return new PricedLine(input.productId(), input.quantity(), money(input.listPrice()), discountPerUnit,
                discountAmount, best == null ? null : best.title(), lineTotal, taxableValue,
                rate.setScale(2, RoundingMode.HALF_UP), cgst, sgst, igst);
    }

    // The single offer that saves the most per unit. Offers never stack.
    static OfferTerms bestOffer(LineInput input)
    {
        OfferTerms best = null;
        BigDecimal bestDiscount = BigDecimal.ZERO;
        for (OfferTerms offer : input.offers()) {
            if (!offer.appliesTo(input.productId(), input.quantity())) 
            {
                continue;
            }
            BigDecimal discount = offer.discountPerUnit(input.listPrice());
            if (discount.compareTo(bestDiscount) > 0)
            {
                best = offer;
                bestDiscount = discount;
            }
        }
        return best;
    }

    private static BigDecimal zero() 
    {
        return BigDecimal.ZERO.setScale(2);
    }

    private static BigDecimal money(BigDecimal value) 
    {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
