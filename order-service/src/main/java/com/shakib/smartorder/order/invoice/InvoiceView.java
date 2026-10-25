package com.shakib.smartorder.order.invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/*
 Everything printed on a bill. It is also the JSON answer of the invoice endpoint, so the PDF and
 the API can never disagree. All amounts come from the order snapshot saved when it was placed.
 */
public record InvoiceView(
        String invoiceNumber,
        Instant issuedAt,
        Long orderId,
        Instant orderDate,
        String orderStatus,
        String orderType,
        String currency,
        Party seller,
        Party buyer,
        String placeOfSupply,
        boolean intraState,
        List<Line> lines,
        List<TaxSlab> taxSummary,
        Totals totals,
        String paymentMethod,
        String paymentStatus,
        String trackingNumber,
        String amountInWords) {

    public record Party(String name, String tradeName, String gstin, String phone, List<String> addressLines)
    {
    }

    public record Line(int number, String description, String details, String hsnCode, int quantity,
            BigDecimal listPrice, BigDecimal discount, String offerTitle, BigDecimal taxableValue,
            BigDecimal gstRatePercent, BigDecimal gstAmount, BigDecimal total) {
    }

    // GST summed per rate, the table every GST invoice carries.
    public record TaxSlab(BigDecimal ratePercent, BigDecimal taxableValue, BigDecimal cgst, BigDecimal sgst,
            BigDecimal igst) {
    }

    public record Totals(BigDecimal subtotal, BigDecimal discount, BigDecimal taxableValue, BigDecimal cgst,
            BigDecimal sgst, BigDecimal igst, BigDecimal shipping, BigDecimal grandTotal, BigDecimal youSaved) {
    }
}
