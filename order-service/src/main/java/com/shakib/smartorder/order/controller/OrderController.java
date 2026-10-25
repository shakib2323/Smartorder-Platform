package com.shakib.smartorder.order.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shakib.smartorder.order.dto.CreateOrderRequest;
import com.shakib.smartorder.order.dto.OrderResponse;
import com.shakib.smartorder.order.dto.OrderSummaryResponse;
import com.shakib.smartorder.order.dto.PageResponse;
import com.shakib.smartorder.order.dto.PlacementResult;
import com.shakib.smartorder.order.enums.OrderStatus;
import com.shakib.smartorder.order.invoice.InvoiceService;
import com.shakib.smartorder.order.invoice.InvoiceView;
import com.shakib.smartorder.order.security.Caller;
import com.shakib.smartorder.order.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService service;
    private final InvoiceService invoiceService;

    public OrderController(OrderService service, InvoiceService invoiceService) {
        this.service = service;
        this.invoiceService = invoiceService;
    }

    // Places an order for ONE brand. 202 = accepted, stock is being reserved; 200 = same Idempotency-Key replayed. 
    @PostMapping
    public ResponseEntity<OrderResponse> place(@RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request,@AuthenticationPrincipal Jwt jwt) 
    {
        PlacementResult result = service.placeOrder(idempotencyKey, request, Caller.id(jwt));
        HttpStatus status = result.replayed() ? HttpStatus.OK : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status)
                .location(URI.create("/api/v1/orders/" + result.order().id()))
                .body(result.order());
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        if (Caller.isAdmin(authentication))
        {
            return service.get(id);
        }
        return service.getForCustomer(id, Caller.id(jwt));
    }

    @GetMapping
    public PageResponse<OrderSummaryResponse> list( @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,@RequestParam(required = false) OrderStatus status,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) 
    {
        if (Caller.isAdmin(authentication))
        {
            return service.list(page, size, status);
        }
        return service.listForCustomer(Caller.id(jwt), page, size, status);
    }

    // Cancels a CONFIRMED or RESERVED order and releases its stock.
    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) 
    {
        if (Caller.isAdmin(authentication)) 
        {
            return service.cancel(id);
        }
        return service.cancelForCustomer(id, Caller.id(jwt));
    }

    // Turns a RESERVED order into a CONFIRMED one. Refused after the hold time has run out.
    @PostMapping("/{id}/confirm-reservation")
    public OrderResponse confirmReservation(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) 
    {
        return service.confirmReservation(id, Caller.id(jwt));
    }

    //The bill as JSON. Available once the order is confirmed.
    @GetMapping("/{id}/invoice")
    public InvoiceView invoice(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt,
            Authentication authentication)
    {
        return invoiceService.view(id, viewer(jwt, authentication));
    }

    // The bill as a PDF, ready to print.
    @GetMapping("/{id}/invoice.pdf")
    public ResponseEntity<byte[]> invoicePdf(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) 
    {
        return PdfResponses.inline(invoiceService.pdf(id, viewer(jwt, authentication)));
    }

    private InvoiceService.Viewer viewer(Jwt jwt, Authentication authentication)
    {
        return Caller.isAdmin(authentication) ? InvoiceService.Viewer.administrator()
                : InvoiceService.Viewer.customer(Caller.id(jwt));
    }
}
