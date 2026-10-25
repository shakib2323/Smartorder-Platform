package com.shakib.smartorder.order.cart.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shakib.smartorder.order.billing.LineInput;
import com.shakib.smartorder.order.billing.PricedLine;
import com.shakib.smartorder.order.billing.PricingEngine;
import com.shakib.smartorder.order.cart.dto.AddCartItemRequest;
import com.shakib.smartorder.order.cart.dto.CartLineResponse;
import com.shakib.smartorder.order.cart.dto.CartResponse;
import com.shakib.smartorder.order.cart.entity.CartItem;
import com.shakib.smartorder.order.cart.repository.CartItemRepository;
import com.shakib.smartorder.order.client.ProductClient;
import com.shakib.smartorder.order.dto.ProductInfo;
import com.shakib.smartorder.order.exception.InvalidRequestException;
import com.shakib.smartorder.order.exception.ProductInactiveException;
import com.shakib.smartorder.order.exception.ProductNotFoundException;

@Service
public class CartService {

    private final CartItemRepository cartRepository;
    private final ProductClient productClient;

    public CartService(CartItemRepository cartRepository, ProductClient productClient) {
        this.cartRepository = cartRepository;
        this.productClient = productClient;
    }

    @Transactional
    public CartResponse addItem(Long customerId, AddCartItemRequest request) 
    {
        if (request.productId() <= 0) {
            throw new InvalidRequestException("productId must be positive");
        }
        requireAvailable(request.productId());

        CartItem item = cartRepository.findByCustomerIdAndProductId(customerId, request.productId())
                .orElseGet(() -> new CartItem(customerId, request.productId(), 0));
        int nextQuantity = item.getQuantity() + request.quantity();
        if (nextQuantity > 99) {
            throw new InvalidRequestException("A cart item quantity cannot exceed 99");
        }
        item.setQuantity(nextQuantity);
        cartRepository.save(item);
        return getCart(customerId);
    }

    /*
      The cart with current prices and the seller offers that apply at each quantity.
      Products are fetched in ONE call. A product that was removed or hidden since it was added is
      shown as unavailable and left out of the totals; it no longer makes the whole cart fail.
     */
    public CartResponse getCart(Long customerId) 
    {
        List<CartItem> items = cartRepository.findAllByCustomerIdOrderByCreatedAtAsc(customerId);
        if (items.isEmpty()) {
            return new CartResponse(List.of(), 0, money(BigDecimal.ZERO), money(BigDecimal.ZERO));
        }
        Map<Long, ProductInfo> products = productClient.getProducts(items.stream().map(CartItem::getProductId).toList());

        List<CartLineResponse> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal savings = BigDecimal.ZERO;
        int totalItems = 0;
        for (CartItem item : items) 
        {
            ProductInfo product = products.get(item.getProductId());
            if (product == null || !product.active()) {
                lines.add(new CartLineResponse(item.getProductId(), "No longer available", null, null,
                        money(BigDecimal.ZERO), money(BigDecimal.ZERO), null, item.getQuantity(),
                        money(BigDecimal.ZERO), false));
                continue;
            }
            // Same pricing rules as an order, so the cart shows what you will actually pay for the items.
            PricedLine priced = PricingEngine.priceLine(new LineInput(product.id(), item.getQuantity(),
                    product.price(), product.gstRatePercent(), product.offerTerms()), true);
            lines.add(new CartLineResponse(product.id(), product.name(),
                    product.brand() == null ? null : product.brand().id(),
                    product.brand() == null ? null : product.brand().name(), priced.listPrice(),
                    priced.discountPerUnit(), priced.offerTitle(), item.getQuantity(), priced.lineTotal(), true));
            total = total.add(priced.lineTotal());
            savings = savings.add(priced.discountAmount());
            totalItems += item.getQuantity();
        }
        return new CartResponse(lines, totalItems, money(total), money(savings));
    }

    @Transactional
    public CartResponse updateQuantity(Long customerId, Long productId, int quantity) 
    {
        CartItem item = cartRepository.findByCustomerIdAndProductId(customerId, productId)
                .orElseThrow(() -> new InvalidRequestException("Product is not in your cart"));
        requireAvailable(productId);
        item.setQuantity(quantity);
        cartRepository.save(item);
        return getCart(customerId);
    }

    @Transactional
    public void removeItem(Long customerId, Long productId)
    {
        cartRepository.deleteByCustomerIdAndProductId(customerId, productId);
    }

    // Removes only these products, so an item added while a checkout was running is not lost.
    @Transactional
    public void removeItems(Long customerId, Collection<Long> productIds) 
    {
        productIds.forEach(id -> cartRepository.deleteByCustomerIdAndProductId(customerId, id));
    }

    @Transactional
    public void clearCart(Long customerId)
    {
        cartRepository.deleteAllByCustomerId(customerId);
    }

    private void requireAvailable(Long productId) 
    {
        ProductInfo product = productClient.getProducts(List.of(productId)).get(productId);
        if (product == null) 
        {
            throw new ProductNotFoundException(productId);
        }
        if (!product.active()) 
        {
            throw new ProductInactiveException(productId);
        }
    }

    private static BigDecimal money(BigDecimal value)
    {
        return value.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
