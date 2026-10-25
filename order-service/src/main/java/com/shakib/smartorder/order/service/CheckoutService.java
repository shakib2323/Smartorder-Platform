package com.shakib.smartorder.order.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.shakib.smartorder.order.cart.entity.CartItem;
import com.shakib.smartorder.order.cart.repository.CartItemRepository;
import com.shakib.smartorder.order.cart.service.CartService;
import com.shakib.smartorder.order.client.ProductClient;
import com.shakib.smartorder.order.dto.CheckoutRequest;
import com.shakib.smartorder.order.dto.CheckoutResponse;
import com.shakib.smartorder.order.dto.CreateOrderRequest;
import com.shakib.smartorder.order.dto.OrderItemRequest;
import com.shakib.smartorder.order.dto.OrderResponse;
import com.shakib.smartorder.order.dto.PlacementResult;
import com.shakib.smartorder.order.dto.ProductInfo;
import com.shakib.smartorder.order.exception.InvalidRequestException;

/*
  Turns the cart into orders: one order per brand, because every seller ships and invoices on their own.
  Retry-safe: each order uses the key "<Idempotency-Key>-b<brandId>", so repeating the call after a failure
  returns the orders already created and only creates the missing ones. The cart is emptied only when every
  order exists. (Not one big database transaction on purpose: the orders belong to different sellers and
  Inventory reserves their stock separately anyway.)
 */
@Service
public class CheckoutService {

    private static final int MAX_KEY_LENGTH = 80; // leaves room for the "-b<brandId>" suffix (column limit 100)

    private final CartItemRepository cartRepository;
    private final CartService cartService;
    private final ProductClient productClient;
    private final OrderService orderService;

    public CheckoutService(CartItemRepository cartRepository, CartService cartService, ProductClient productClient,
            OrderService orderService) {
        this.cartRepository = cartRepository;
        this.cartService = cartService;
        this.productClient = productClient;
        this.orderService = orderService;
    }

    // replayed = true when every order already existed from an earlier call with the same key. 
    public record Result(CheckoutResponse response, boolean replayed) {
    }

    public Result checkout(Long customerId, String idempotencyKey, CheckoutRequest request)
    {
        if (idempotencyKey == null || idempotencyKey.isBlank()) 
        {
            throw new InvalidRequestException("Idempotency-Key header must not be blank");
        }
        if (idempotencyKey.length() > MAX_KEY_LENGTH)
        {
            throw new InvalidRequestException(
                    "Idempotency-Key must be at most " + MAX_KEY_LENGTH + " characters for cart checkout");
        }

        List<CartItem> items = cartRepository.findAllByCustomerIdOrderByCreatedAtAsc(customerId);
        if (items.isEmpty()) {
            throw new InvalidRequestException("Your cart is empty");
        }

        Map<Long, ProductInfo> products = productClient.getProducts(items.stream().map(CartItem::getProductId).toList());
        List<Long> unavailable = items.stream().map(CartItem::getProductId)
                .filter(id -> products.get(id) == null || !products.get(id).active())
                .toList();
        if (!unavailable.isEmpty())
        {
            throw new InvalidRequestException("Some products in your cart are no longer available: " + unavailable
                    + ". Remove them and try again.");
        }

        Map<Long, List<CartItem>> byBrand = new LinkedHashMap<>();
        for (CartItem item : items) {
            ProductInfo.BrandInfo brand = products.get(item.getProductId()).brand();
            if (brand == null || brand.id() == null) 
            {
                throw new InvalidRequestException("Product " + item.getProductId() + " has no seller information");
            }
            byBrand.computeIfAbsent(brand.id(), id -> new ArrayList<>()).add(item);
        }

        List<OrderResponse> orders = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO.setScale(2);
        boolean anyNew = false;
        for (Map.Entry<Long, List<CartItem>> group : byBrand.entrySet())
        {
            List<OrderItemRequest> lines = group.getValue().stream()
                    .map(i -> new OrderItemRequest(i.getProductId(), i.getQuantity()))
                    .toList();
            CreateOrderRequest orderRequest = new CreateOrderRequest(lines, request.shippingAddress(), request.type(),
                    request.holdHours());
            PlacementResult placed = orderService.placeOrder(idempotencyKey + "-b" + group.getKey(), orderRequest,
                    customerId, products);
            anyNew |= !placed.replayed();
            orders.add(placed.order());
            grandTotal = grandTotal.add(placed.order().totalAmount());
        }

        cartService.removeItems(customerId, items.stream().map(CartItem::getProductId).toList());
        return new Result(new CheckoutResponse(orders, grandTotal), !anyNew);
    }
}
