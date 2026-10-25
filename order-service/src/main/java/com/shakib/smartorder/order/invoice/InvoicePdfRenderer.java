package com.shakib.smartorder.order.invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.shakib.smartorder.order.billing.MoneyFormat;
import com.shakib.smartorder.order.invoice.InvoiceView.Line;
import com.shakib.smartorder.order.invoice.InvoiceView.Party;
import com.shakib.smartorder.order.invoice.InvoiceView.TaxSlab;
import com.shakib.smartorder.order.invoice.InvoiceView.Totals;
import com.shakib.smartorder.order.invoice.PdfDocument.Page;

// Draws an {@link InvoiceView} as an A4 PDF: header, parties, item table, GST summary, totals, footer.
@Component
public class InvoicePdfRenderer {

    // Colours
    private static final int NAVY = 0x1F2A44;
    private static final int TEAL = 0x0E7C86;
    private static final int PANEL = 0xF3F5F8;
    private static final int BORDER = 0xD5DAE1;
    private static final int TEXT = 0x1F2937;
    private static final int MUTED = 0x6B7280;
    private static final int GREEN = 0x15803D;
    private static final int GREEN_BG = 0xE8F5EC;
    private static final int RED = 0xB91C1C;
    private static final int WHITE = 0xFFFFFF;

    // Layout (points)
    private static final double LEFT = 36;
    private static final double RIGHT = PdfDocument.PAGE_WIDTH - 36;
    private static final double CONTENT_WIDTH = RIGHT - LEFT;
    private static final double TABLE_BOTTOM = 770;
    private static final double TOTALS_BLOCK_HEIGHT = 235;
    private static final double ROW_HEIGHT = 30;

    // Item table columns: # | Item | HSN | Qty | Rate | Discount | Taxable | GST | Amount (sum = 523)
    private static final double[] COL_WIDTH = { 22, 150, 44, 28, 62, 56, 62, 40, 59 };

    // Invoices are issued in Indian time.
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
            .withZone(ZoneId.of("Asia/Kolkata"));

    public byte[] render(InvoiceView invoice)
    {
        PdfDocument doc = new PdfDocument();
        doc.setTitle("Invoice " + invoice.invoiceNumber());
        String currency = invoice.currency();

        Page page = doc.newPage();
        double y = drawHeader(page, invoice);
        y = drawParties(page, invoice, y + 14);
        y = drawMeta(page, invoice, y + 10);

        y = drawTableHeader(page, y + 12);
        int rowIndex = 0;
        for (Line line : invoice.lines())
        {
            if (y + ROW_HEIGHT > TABLE_BOTTOM)
            {
                page = doc.newPage();
                y = drawTableHeader(page, 50);
            }
            drawRow(page, line, y, rowIndex++ % 2 == 1, currency);
            y += ROW_HEIGHT;
        }
        page.line(LEFT, y, RIGHT, y, 0.8, BORDER);

        if (y + TOTALS_BLOCK_HEIGHT > TABLE_BOTTOM + 20) {
            page = doc.newPage();
            y = 50 - 14;
        }
        drawTotalsBlock(page, invoice, y + 14);

        drawFooters(doc, invoice);
        return doc.toBytes();
    }

    //  header

    private double drawHeader(Page page, InvoiceView inv) 
    {
        page.fillRect(0, 0, PdfDocument.PAGE_WIDTH, 80, NAVY);
        page.text(LEFT, 40, true, 22, "TAX INVOICE", WHITE);
        page.text(LEFT, 60, false, 10, "SmartOrder Marketplace", 0xC9D3E6);

        page.textRight(RIGHT, 32, true, 13, inv.invoiceNumber(), WHITE);
        page.textRight(RIGHT, 49, false, 9.5, "Invoice date: " + DATE.format(inv.issuedAt()), 0xC9D3E6);
        page.textRight(RIGHT, 63, false, 9.5, "Order #" + inv.orderId() + "  |  " + DATE.format(inv.orderDate()),
                0xC9D3E6);

        if ("CANCELLED".equals(inv.orderStatus()))
        {
            // Inside the header band, where nothing is drawn on top of it afterwards.
            page.fillRect(250, 26, 108, 28, RED);
            page.textCenter(304, 45, true, 13, "CANCELLED", WHITE);
        }
        return 80;
    }

    // parties

    private double drawParties(Page page, InvoiceView inv, double top) 
    {
        double boxWidth = (CONTENT_WIDTH - 12) / 2;
        List<String> sellerLines = partyLines(inv.seller(), true);
        List<String> buyerLines = partyLines(inv.buyer(), false);
        int rows = Math.max(sellerLines.size(), buyerLines.size());
        double height = 30 + rows * 13 + 6;

        drawParty(page, LEFT, top, boxWidth, height, "SOLD BY", sellerLines);
        drawParty(page, LEFT + boxWidth + 12, top, boxWidth, height, "BILL TO / SHIP TO", buyerLines);
        return top + height;
    }

    private void drawParty(Page page, double x, double top, double width, double height, String title,
            List<String> lines) 
    {
        page.fillRect(x, top, width, height, PANEL);
        page.fillRect(x, top, 3, height, TEAL);
        page.text(x + 12, top + 16, true, 8, title, MUTED);
        double y = top + 31;
        boolean first = true;
        for (String line : lines) {
            page.text(x + 12, y, first, first ? 10.5 : 9, PdfDocument.fit(line, first, first ? 10.5 : 9, width - 22),
                    TEXT);
            first = false;
            y += 13;
        }
    }

    private List<String> partyLines(Party party, boolean seller)
    {
        List<String> lines = new ArrayList<>();
        lines.add(party.name());
        if (party.tradeName() != null && !party.tradeName().equals(party.name())) 
        {
            lines.add("Brand: " + party.tradeName());
        }
        if (seller) 
        {
            lines.add("GSTIN: " + (party.gstin() == null ? "Not registered" : party.gstin()));
        } else if (party.phone() != null) {
            lines.add("Phone: " + party.phone());
        }
        lines.addAll(party.addressLines());
        return lines;
    }

    private double drawMeta(Page page, InvoiceView inv, double top) 
    {
        String supply = "Place of supply: " + inv.placeOfSupply() + (inv.intraState()
                ? "  (same state: CGST + SGST)" : "  (other state: IGST)");
        page.text(LEFT, top + 8, false, 8.5, supply, MUTED);
        String payment = "Payment: " + inv.paymentMethod() + " - " + inv.paymentStatus()
                + (inv.trackingNumber() == null ? "" : "   |   Tracking: " + inv.trackingNumber());
        page.textRight(RIGHT, top + 8, false, 8.5, payment, MUTED);
        return top + 12;
    }

    //  item table

    private double drawTableHeader(Page page, double top) 
    {
        page.fillRect(LEFT, top, CONTENT_WIDTH, 20, NAVY);
        String[] titles = {
        		"#", "Item", "HSN", "Qty", "Rate", "Discount", "Taxable", "GST", "Amount"
        		};
        double x = LEFT;
        for (int i = 0; i < titles.length; i++) 
        {
            if (i <= 1)
            {
                page.text(x + 4, top + 13.5, true, 8, titles[i], WHITE);
            } else {
                page.textRight(x + COL_WIDTH[i] - 4, top + 13.5, true, 8, titles[i], WHITE);
            }
            x += COL_WIDTH[i];
        }
        return top + 20;
    }

    private void drawRow(Page page, Line line, double top, boolean shaded, String currency) 
    {
        if (shaded) 
        {
            page.fillRect(LEFT, top, CONTENT_WIDTH, ROW_HEIGHT, PANEL);
        }
        double x = LEFT;
        double baseline = top + 12;

        page.text(x + 4, baseline, false, 8.5, String.valueOf(line.number()), MUTED);
        x += COL_WIDTH[0];

        page.text(x + 4, baseline, true, 8.5, PdfDocument.fit(line.description(), true, 8.5, COL_WIDTH[1] - 8), TEXT);
        if (line.details() != null) 
        {
            page.text(x + 4, baseline + 10.5, false, 7, PdfDocument.fit(line.details(), false, 7, COL_WIDTH[1] - 8),
                    MUTED);
        }
        x += COL_WIDTH[1];

        page.textRight(x + COL_WIDTH[2] - 4, baseline, false, 8, line.hsnCode() == null ? "-" : line.hsnCode(), TEXT);
        x += COL_WIDTH[2];

        page.textRight(x + COL_WIDTH[3] - 4, baseline, false, 8.5, String.valueOf(line.quantity()), TEXT);
        x += COL_WIDTH[3];

        page.textRight(x + COL_WIDTH[4] - 4, baseline, false, 8.5, MoneyFormat.number(line.listPrice(), currency), TEXT);
        x += COL_WIDTH[4];

        boolean discounted = line.discount().signum() > 0;
        page.textRight(x + COL_WIDTH[5] - 4, baseline, false, 8.5,
                discounted ? "-" + MoneyFormat.number(line.discount(), currency) : "-", discounted ? GREEN : MUTED);
        if (discounted && line.offerTitle() != null) {
            page.textRight(x + COL_WIDTH[5] - 4, baseline + 10.5, false, 6.5,
                    PdfDocument.fit(line.offerTitle(), false, 6.5, COL_WIDTH[5] - 6), GREEN);
        }
        x += COL_WIDTH[5];

        page.textRight(x + COL_WIDTH[6] - 4, baseline, false, 8.5, MoneyFormat.number(line.taxableValue(), currency),
                TEXT);
        x += COL_WIDTH[6];

        page.textRight(x + COL_WIDTH[7] - 4, baseline, false, 8.5, percent(line.gstRatePercent()), TEXT);
        x += COL_WIDTH[7];

        page.textRight(x + COL_WIDTH[8] - 4, baseline, true, 8.5, MoneyFormat.number(line.total(), currency), TEXT);
    }

    // totals

    private void drawTotalsBlock(Page page, InvoiceView inv, double top)
    {
        String currency = inv.currency();
        Totals t = inv.totals();

        // right: totals box
        double boxWidth = 215;
        double boxX = RIGHT - boxWidth;
        double y = top + 4;
        y = totalsLine(page, boxX, boxWidth, y, "Items total (before offers)", t.subtotal(), currency, TEXT, false);
        if (t.discount().signum() > 0) {
            y = totalsLine(page, boxX, boxWidth, y, "Offer discount", t.discount().negate(), currency, GREEN, false);
        }
        y = totalsLine(page, boxX, boxWidth, y, "Taxable value", t.taxableValue(), currency, TEXT, false);
        if (inv.intraState()) {
            y = totalsLine(page, boxX, boxWidth, y, "CGST", t.cgst(), currency, TEXT, false);
            y = totalsLine(page, boxX, boxWidth, y, "SGST", t.sgst(), currency, TEXT, false);
        } else {
            y = totalsLine(page, boxX, boxWidth, y, "IGST", t.igst(), currency, TEXT, false);
        }
        y = totalsLine(page, boxX, boxWidth, y, "Delivery charges",
                t.shipping(), currency, TEXT, t.shipping().signum() == 0);
        y += 4;
        page.fillRect(boxX, y, boxWidth, 28, TEAL);
        page.text(boxX + 10, y + 18, true, 10.5, "GRAND TOTAL", WHITE);
        page.textRight(boxX + boxWidth - 10, y + 18, true, 12, MoneyFormat.withSymbol(t.grandTotal(), currency), WHITE);

        // left: GST summary, amount in words, savings
        double leftWidth = boxX - LEFT - 18;
        double ly = top + 4;
        ly = drawTaxSummary(page, inv, ly, leftWidth);

        if (inv.amountInWords() != null) {
            ly += 12;
            page.text(LEFT, ly, true, 8, "AMOUNT IN WORDS", MUTED);
            for (String wrapped : wrap(inv.amountInWords(), leftWidth, 9)) {
                ly += 12;
                page.text(LEFT, ly, false, 9, wrapped, TEXT);
            }
        }
        if (t.youSaved().signum() > 0) {
            ly += 14;
            page.fillRect(LEFT, ly - 11, leftWidth, 22, GREEN_BG);
            page.text(LEFT + 8, ly + 3, true, 9.5, "You saved " + MoneyFormat.withSymbol(t.youSaved(), currency)
                    + " on this order with seller offers!", GREEN);
        }
    }

    private double totalsLine(Page page, double x, double width, double y, String label, BigDecimal amount,
            String currency, int color, boolean showFree)
    {
        page.text(x + 10, y + 14, false, 9, label, MUTED);
        page.textRight(x + width - 10, y + 14, false, 9, showFree ? "FREE" : MoneyFormat.number(amount, currency),
                showFree ? GREEN : color);
        page.line(x, y + 20, x + width, y + 20, 0.4, BORDER);
        return y + 21;
    }

    private double drawTaxSummary(Page page, InvoiceView inv, double top, double width) {
        String currency = inv.currency();
        String[] titles = inv.intraState()
                ? new String[] { "GST rate", "Taxable", "CGST", "SGST", "Total tax" }
                : new String[] { "GST rate", "Taxable", "IGST", "", "Total tax" };
        double col = width / 5;
        page.fillRect(LEFT, top, width, 16, PANEL);
        for (int i = 0; i < 5; i++) {
            if (!titles[i].isEmpty()) {
                page.textRight(LEFT + col * (i + 1) - 4, top + 11, true, 7.5, titles[i], MUTED);
            }
        }
        double y = top + 16;
        for (TaxSlab slab : inv.taxSummary()) {
            BigDecimal totalTax = slab.cgst().add(slab.sgst()).add(slab.igst());
            String[] cells = inv.intraState()
                    ? new String[] { percent(slab.ratePercent()), MoneyFormat.number(slab.taxableValue(), currency),
                            MoneyFormat.number(slab.cgst(), currency), MoneyFormat.number(slab.sgst(), currency),
                            MoneyFormat.number(totalTax, currency) }
                    : new String[] { percent(slab.ratePercent()), MoneyFormat.number(slab.taxableValue(), currency),
                            MoneyFormat.number(slab.igst(), currency), "", MoneyFormat.number(totalTax, currency) };
            for (int i = 0; i < 5; i++) {
                if (!cells[i].isEmpty()) {
                    page.textRight(LEFT + col * (i + 1) - 4, y + 11, false, 8, cells[i], TEXT);
                }
            }
            y += 15;
            page.line(LEFT, y, LEFT + width, y, 0.4, BORDER);
        }
        return y;
    }

    // footer

    private void drawFooters(PdfDocument doc, InvoiceView inv) {
        // Pages were created in order; the footer needs the total page count, so it is drawn last.
        for (int i = 0; i < doc.pageCount(); i++) {
            Page page = doc.pageAt(i);
            page.line(LEFT, 800, RIGHT, 800, 0.6, BORDER);
            page.text(LEFT, 814, false, 7.5,
                    "This is a computer-generated invoice and does not need a signature.", MUTED);
            page.text(LEFT, 825, false, 7.5,
                    "Invoice " + inv.invoiceNumber() + "  |  Sold by " + inv.seller().name()
                            + "  |  Thank you for shopping with SmartOrder.", MUTED);
            page.textRight(RIGHT, 814, true, 8, "Page " + (i + 1) + " of " + doc.pageCount(), MUTED);
        }
    }

    //helpers

    private static String percent(BigDecimal rate) 
    {
        BigDecimal stripped = rate.stripTrailingZeros();
        return (stripped.scale() < 0 ? stripped.setScale(0) : stripped).toPlainString() + "%";
    }

    private static List<String> wrap(String text, double maxWidth, double size) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" "))
        {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (PdfDocument.width(candidate, false, size) > maxWidth && current.length() > 0) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    //Kept for symmetry with other renderers: the document date used in tests.
    static String formatDate(Instant instant) 
    {
        return DATE.format(instant);
    }
}
