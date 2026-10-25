package com.shakib.smartorder.order.cart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shakib.smartorder.order.cart.dto.AddCartItemRequest;
import com.shakib.smartorder.order.cart.dto.CartResponse;
import com.shakib.smartorder.order.cart.entity.CartItem;
import com.shakib.smartorder.order.cart.repository.CartItemRepository;
import com.shakib.smartorder.order.cart.service.CartService;
import com.shakib.smartorder.order.client.ProductClient;
import com.shakib.smartorder.order.dto.ProductInfo;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartRepository;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private CartService cartService;

    @Test
    void addingProductUpdatesOnlyTheAuthenticatedCustomersCart() {
        Long customerId = 42L;
        Long productId = 7L;
        when(productClient.getProducts(anyCollection())).thenReturn(
                Map.of(productId, ProductInfo.basic(productId, "Keyboard", new BigDecimal("25.00"), true)));
        AtomicReference<CartItem> stored = new AtomicReference<>();
        when(cartRepository.findByCustomerIdAndProductId(customerId, productId)).thenReturn(Optional.empty());
        when(cartRepository.save(any(CartItem.class))).thenAnswer(invocation -> {
            stored.set(invocation.getArgument(0));
            return stored.get();
        });
        when(cartRepository.findAllByCustomerIdOrderByCreatedAtAsc(customerId))
                .thenAnswer(invocation -> stored.get() == null ? List.of() : List.of(stored.get()));

        CartResponse response = cartService.addItem(customerId, new AddCartItemRequest(productId, 2));

        verify(cartRepository).findByCustomerIdAndProductId(customerId, productId);
        verify(cartRepository).save(any(CartItem.class));
        assertEquals(customerId, stored.get().getCustomerId());
        assertEquals(2, stored.get().getQuantity());
        assertEquals(2, response.totalItems());
        assertEquals(0, new BigDecimal("50.00").compareTo(response.totalAmount()));
    }

    @Test
    void aProductThatDisappearedDoesNotBreakTheWholeCart() 
    {
        Long customerId = 42L;
        when(cartRepository.findAllByCustomerIdOrderByCreatedAtAsc(customerId))
                .thenReturn(List.of(new CartItem(customerId, 1L, 1), new CartItem(customerId, 2L, 3)));
        // Product 2 was deactivated: the Product service no longer returns it.
        when(productClient.getProducts(anyCollection())).thenReturn(
                Map.of(1L, ProductInfo.basic(1L, "Mug", new BigDecimal("10.00"), true)));

        CartResponse response = cartService.getCart(customerId);

        assertEquals(2, response.items().size());
        assertTrue(response.items().get(0).available());
        assertFalse(response.items().get(1).available());
        assertEquals(1, response.totalItems());
        assertEquals(0, new BigDecimal("10.00").compareTo(response.totalAmount()));
    }
}
