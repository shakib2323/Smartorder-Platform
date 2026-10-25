package com.shakib.smartorder.order.invoice;

import java.math.BigDecimal;
import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.shakib.smartorder.order.billing.AmountInWords;
import com.shakib.smartorder.order.billing.PricingEngine;
import com.shakib.smartorder.order.entity.Invoice;
import com.shakib.smartorder.order.entity.Order;
import com.shakib.smartorder.order.entity.OrderItem;
import com.shakib.smartorder.order.entity.OrderTotals;
import com.shakib.smartorder.order.entity.SellerSnapshot;
import com.shakib.smartorder.order.entity.ShippingAddress;
import com.shakib.smartorder.order.enums.OrderStatus;
import com.shakib.smartorder.order.exception.InvalidOrderStateException;
import com.shakib.smartorder.order.exception.OrderNotFoundException;
import com.shakib.smartorder.order.invoice.InvoiceView.Line;
import com.shakib.smartorder.order.invoice.InvoiceView.Party;
import com.shakib.smartorder.order.invoice.InvoiceView.TaxSlab;
import com.shakib.smartorder.order.invoice.InvoiceView.Totals;
import com.shakib.smartorder.order.repository.InvoiceRepository;
import com.shakib.smartorder.order.repository.OrderRepository;

/*
  Builds the bill of an order. The invoice number is assigned the first time the bill is requested after
  the order is confirmed, and never changes. Every amount is read from the snapshot saved with the order,
  so the bill stays the same even if the seller edits their brand or the product price changes later.
 */
@Service
public class InvoiceService {

    // An invoice can only be issued for an order that is really going ahead.
    private static final Set<OrderStatus> INVOICEABLE =
            EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    private final TransactionTemplate transactionTemplate;
    private final InvoicePdfRenderer pdfRenderer;

    public InvoiceService(OrderRepository orderRepository, InvoiceRepository invoiceRepository,
            TransactionTemplate transactionTemplate, InvoicePdfRenderer pdfRenderer) {
        this.orderRepository = orderRepository;
        this.invoiceRepository = invoiceRepository;
        this.transactionTemplate = transactionTemplate;
        this.pdfRenderer = pdfRenderer;
    }

    // Who is asking. Exactly one of the three applies: the buyer, the seller, or an admin.
    public record Viewer(Long customerId, Long sellerId, boolean admin) {
        public static Viewer customer(long id) { 
        	return new Viewer(id, null, false); 
    }
        public static Viewer seller(long id) { 
        	return new Viewer(null, id, false); 
    }
        public static Viewer administrator() { 
        	return new Viewer(null, null, true); 
     }
    }

    public record Document(String fileName, byte[] bytes) {
    }

    public InvoiceView view(Long orderId, Viewer viewer) {
        Order order = load(orderId, viewer);
        return toView(order, invoiceFor(order));
    }

    public Document pdf(Long orderId, Viewer viewer) {
        InvoiceView view = view(orderId, viewer);
        return new Document(view.invoiceNumber() + ".pdf", pdfRenderer.render(view));
    }

    //loading and numbering

    private Order load(Long orderId, Viewer viewer) 
    {
        Optional<Order> order;
        if (viewer.admin()) 
        {
            order = orderRepository.findWithItemsById(orderId);
        } else if (viewer.sellerId() != null) 
        {
            order = orderRepository.findWithItemsByIdAndSellerId(orderId, viewer.sellerId());
        } else {
            order = orderRepository.findWithItemsByIdAndCustomerId(orderId, viewer.customerId());
        }
        return order.orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private Invoice invoiceFor(Order order) 
    {
        Optional<Invoice> existing = invoiceRepository.findByOrderId(order.getId());
        if (existing.isPresent())
        {
            return existing.get(); // also covers an order cancelled after its invoice was issued
        }
        if (!INVOICEABLE.contains(order.getStatus())) 
        {
            throw new InvalidOrderStateException(order.getId(), order.getStatus(),
                    "invoiced (an invoice is issued once the order is confirmed)");
        }
        try {
            return transactionTemplate.execute(
                    status -> invoiceRepository.save(new Invoice(order.getId(), numberFor(order.getId()))));
        } catch (DataIntegrityViolationException e) {
            // Two requests asked at the same moment: the unique key let one insert win. Use that one.
            return invoiceRepository.findByOrderId(order.getId()).orElseThrow(() -> e);
        }
    }

    // INV-2026-00000042: the year of issue and the order id, so the number is unique and easy to trace back. */
    static String numberFor(Long orderId) 
    {
        return "INV-" + Year.now(INDIA).getValue() + "-" + String.format("%08d", orderId);
    }

    // building the view

    private InvoiceView toView(Order order, Invoice invoice)
    {
        String currency = order.getCurrency();
        ShippingAddress shipTo = order.getShippingAddress();
        SellerSnapshot seller = order.getSeller();
        OrderTotals totals = order.getTotals();
        boolean intraState = seller != null && PricingEngine.isIntraState(seller.getState(), shipTo.getState());

        List<Line> lines = new ArrayList<>();
        Map<BigDecimal, BigDecimal[]> slabs = new TreeMap<>(); // rate -> [taxable, cgst, sgst, igst]
        int number = 1;
        for (OrderItem item : order.getItems()) {
            BigDecimal gst = item.getCgstAmount().add(item.getSgstAmount()).add(item.getIgstAmount());
            lines.add(new Line(number++, item.getProductName(), details(item), item.getHsnCode(),
                    item.getQuantity(), item.getUnitPrice(), item.getDiscountAmount(), item.getOfferTitle(),
                    item.getTaxableValue(), item.getGstRatePercent(), gst, item.getLineTotal()));

            BigDecimal[] slab = slabs.computeIfAbsent(item.getGstRatePercent(), rate -> new BigDecimal[] {
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO });
            slab[0] = slab[0].add(item.getTaxableValue());
            slab[1] = slab[1].add(item.getCgstAmount());
            slab[2] = slab[2].add(item.getSgstAmount());
            slab[3] = slab[3].add(item.getIgstAmount());
        }
        List<TaxSlab> taxSummary = new ArrayList<>();
        slabs.forEach((rate, s) -> taxSummary.add(new TaxSlab(rate, s[0], s[1], s[2], s[3])));

        Totals invoiceTotals = new Totals(totals.getSubtotal(), totals.getDiscountTotal(), totals.getTaxableTotal(),
                totals.getCgstTotal(), totals.getSgstTotal(), totals.getIgstTotal(), totals.getShippingFee(),
                order.getTotalAmount(), totals.getDiscountTotal());

        return new InvoiceView(invoice.getInvoiceNumber(), invoice.getIssuedAt(), order.getId(), order.getCreatedAt(),
                order.getStatus().name(), order.getOrderType().name(), currency, sellerParty(seller), buyerParty(shipTo),
                shipTo.getState(), intraState, lines, taxSummary, invoiceTotals,
                "COD".equals(order.getPaymentMethod()) ? "Cash on delivery" : order.getPaymentMethod(),
                paymentStatus(order.getStatus()), order.getTrackingNumber(),
                AmountInWords.rupees(order.getTotalAmount(), currency));
    }

    private static String details(OrderItem item) 
    {
        List<String> parts = new ArrayList<>();
        if (item.getSize() != null) {
            parts.add("Size " + item.getSize());
        }
        if (item.getColor() != null) {
            parts.add("Color " + item.getColor());
        }
        if (item.getSku() != null) {
            parts.add("SKU " + item.getSku());
        }
        return parts.isEmpty() ? null : String.join(" | ", parts);
    }

    private static Party sellerParty(SellerSnapshot seller) 
    {
        if (seller == null || seller.getLegalName() == null)
        {
            return new Party("SmartOrder Marketplace", null, null, null, List.of());
        }
        List<String> address = new ArrayList<>();
        if (seller.getCity() != null && seller.getState() != null) 
        {
            address.add(seller.getCity() + ", " + seller.getState());
        }
        return new Party(seller.getLegalName(), seller.getBrandName(), seller.getGstin(), null, address);
    }

    private static Party buyerParty(ShippingAddress a) 
    {
        List<String> address = new ArrayList<>();
        address.add(a.getLine1());
        if (a.getLine2() != null) {
            address.add(a.getLine2());
        }
        address.add(a.getCity() + ", " + a.getState() + " - " + a.getPostalCode());
        address.add(a.getCountry());
        return new Party(a.getFullName(), null, null, a.getPhone(), address);
    }

    private static String paymentStatus(OrderStatus status)
    {
        return switch (status) {
            case DELIVERED -> "Paid";
            case CANCELLED -> "Cancelled";
            default -> "Pay on delivery";
        };
    }
}
