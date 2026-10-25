package com.shakib.smartorder.order.cart.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shakib.smartorder.order.cart.dto.AddCartItemRequest;
import com.shakib.smartorder.order.cart.dto.CartResponse;
import com.shakib.smartorder.order.cart.dto.UpdateCartItemRequest;
import com.shakib.smartorder.order.cart.service.CartService;
import com.shakib.smartorder.order.dto.CheckoutRequest;
import com.shakib.smartorder.order.dto.CheckoutResponse;
import com.shakib.smartorder.order.security.Caller;
import com.shakib.smartorder.order.service.CheckoutService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;
    private final CheckoutService checkoutService;

    public CartController(CartService cartService, CheckoutService checkoutService) {
        this.cartService = cartService;
        this.checkoutService = checkoutService;
    }

    /*
      Orders everything in the cart: one order per brand (each seller ships and invoices separately).
      Safe to retry with the same Idempotency-Key. 202 = accepted; 200 = every order already existed.
     */
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CheckoutRequest request, @AuthenticationPrincipal Jwt jwt) 
    {
        CheckoutService.Result result = checkoutService.checkout(customerId(jwt), idempotencyKey, request);
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.ACCEPTED).body(result.response());
    }

    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal Jwt jwt) 
    {
        return cartService.getCart(customerId(jwt));
    }

    @PostMapping("/items")
    public CartResponse addItem(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddCartItemRequest request) 
    {
        return cartService.addItem(customerId(jwt), request);
    }

    @PutMapping("/items/{productId}")
    public CartResponse updateQuantity(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId,
                                       @Valid @RequestBody UpdateCartItemRequest request) 
    {
        return cartService.updateQuantity(customerId(jwt), productId, request.quantity());
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> removeItem(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId) 
    {
        cartService.removeItem(customerId(jwt), productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal Jwt jwt) 
    {
        cartService.clearCart(customerId(jwt));
        return ResponseEntity.noContent().build();
    }

    private Long customerId(Jwt jwt) 
    {
        return Caller.id(jwt);
    }
}
