package com.shakib.smartorder.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shakib.smartorder.order.dto.OrderResponse;
import com.shakib.smartorder.order.dto.OrderSummaryResponse;
import com.shakib.smartorder.order.dto.PageResponse;
import com.shakib.smartorder.order.dto.SellerSummaryResponse;
import com.shakib.smartorder.order.dto.ShipRequest;
import com.shakib.smartorder.order.enums.OrderStatus;
import com.shakib.smartorder.order.invoice.InvoiceService;
import com.shakib.smartorder.order.invoice.InvoiceView;
import com.shakib.smartorder.order.security.Caller;
import com.shakib.smartorder.order.service.OrderService;

import jakarta.validation.Valid;

// What a seller sees and does with the orders for their own brand. 
@RestController
@RequestMapping("/api/v1/seller/orders")
public class SellerOrderController {

    private final OrderService orderService;
    private final InvoiceService invoiceService;

    public SellerOrderController(OrderService orderService, InvoiceService invoiceService) {
        this.orderService = orderService;
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public PageResponse<OrderSummaryResponse> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) OrderStatus status,
            @AuthenticationPrincipal Jwt jwt) 
    {
        return orderService.listForSeller(Caller.id(jwt), page, size, status);
    }

    // Dashboard: how many orders in each status and the total sales.
    @GetMapping("/summary")
    public SellerSummaryResponse summary(@AuthenticationPrincipal Jwt jwt) 
    {
        return orderService.sellerSummary(Caller.id(jwt));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) 
    {
        return orderService.getForSeller(id, Caller.id(jwt));
    }

    // CONFIRMED -> SHIPPED, with an optional tracking number.
    @PostMapping("/{id}/ship")
    public OrderResponse ship(@PathVariable Long id, @Valid @RequestBody(required = false) ShipRequest request,
            @AuthenticationPrincipal Jwt jwt) 
    {
        String tracking = request == null ? null : request.trackingNumber();
        return orderService.ship(id, Caller.id(jwt), tracking);
    }

    // SHIPPED -> DELIVERED.
    @PostMapping("/{id}/deliver")
    public OrderResponse deliver(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) 
    {
        return orderService.deliver(id, Caller.id(jwt));
    }

    @GetMapping("/{id}/invoice")
    public InvoiceView invoice(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) 
    {
        return invoiceService.view(id, InvoiceService.Viewer.seller(Caller.id(jwt)));
    }

    @GetMapping("/{id}/invoice.pdf")
    public ResponseEntity<byte[]> invoicePdf(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt)
    {
        return PdfResponses.inline(invoiceService.pdf(id, InvoiceService.Viewer.seller(Caller.id(jwt))));
    }
}
