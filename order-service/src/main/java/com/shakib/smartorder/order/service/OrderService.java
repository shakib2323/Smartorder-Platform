package com.shakib.smartorder.order.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shakib.smartorder.order.billing.LineInput;
import com.shakib.smartorder.order.billing.PricedOrder;
import com.shakib.smartorder.order.billing.PricingEngine;
import com.shakib.smartorder.order.billing.ShippingPolicy;
import com.shakib.smartorder.order.client.ProductClient;
import com.shakib.smartorder.order.dto.CreateOrderRequest;
import com.shakib.smartorder.order.dto.OrderItemRequest;
import com.shakib.smartorder.order.dto.OrderResponse;
import com.shakib.smartorder.order.dto.OrderSummaryResponse;
import com.shakib.smartorder.order.dto.PageResponse;
import com.shakib.smartorder.order.dto.PlacementResult;
import com.shakib.smartorder.order.dto.ProductInfo;
import com.shakib.smartorder.order.dto.ProductInfo.BrandInfo;
import com.shakib.smartorder.order.dto.SellerSummaryResponse;
import com.shakib.smartorder.order.dto.ShippingAddressRequest;
import com.shakib.smartorder.order.entity.Order;
import com.shakib.smartorder.order.entity.OrderItem;
import com.shakib.smartorder.order.entity.OrderTotals;
import com.shakib.smartorder.order.entity.OutboxEvent;
import com.shakib.smartorder.order.entity.SellerSnapshot;
import com.shakib.smartorder.order.entity.ShippingAddress;
import com.shakib.smartorder.order.enums.OrderStatus;
import com.shakib.smartorder.order.enums.OrderType;
import com.shakib.smartorder.order.events.OrderCancelledEvent;
import com.shakib.smartorder.order.events.OrderCreatedEvent;
import com.shakib.smartorder.order.exception.IdempotencyKeyConflictException;
import com.shakib.smartorder.order.exception.InvalidOrderStateException;
import com.shakib.smartorder.order.exception.InvalidRequestException;
import com.shakib.smartorder.order.exception.OrderNotFoundException;
import com.shakib.smartorder.order.exception.ProductInactiveException;
import com.shakib.smartorder.order.exception.ProductNotFoundException;
import com.shakib.smartorder.order.filter.CorrelationIdFilter;
import com.shakib.smartorder.order.repository.OrderRepository;
import com.shakib.smartorder.order.repository.OutboxEventRepository;

@Service
public class OrderService {

    public static final String ORDER_EVENTS_TOPIC = "order-events";
    public static final String ORDER_CREATED = "OrderCreated";
    public static final String ORDER_CANCELLED = "OrderCancelled";

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxRepository;
    private final ProductClient productClient;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;
    private final ShippingPolicy shippingPolicy;
    private final String currency;
    private final int defaultHoldHours;

    public OrderService(OrderRepository orderRepository, OutboxEventRepository outboxRepository,
            ProductClient productClient, TransactionTemplate transactionTemplate, ObjectMapper objectMapper,
            ShippingPolicy shippingPolicy, @Value("${billing.currency:INR}") String currency,
            @Value("${reservation.default-hold-hours:24}") int defaultHoldHours) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.productClient = productClient;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
        this.shippingPolicy = shippingPolicy;
        this.currency = currency;
        this.defaultHoldHours = defaultHoldHours;
    }

    // placing orders

    /*
      Places an order. Safe to retry with the same Idempotency-Key.
      Not @Transactional on purpose: the remote Product call happens first, outside any database
      transaction, so a slow Product service never holds a database connection.
      Only the final write (order + items + outbox row) runs in one short transaction.
     */
    public PlacementResult placeOrder(String idempotencyKey, CreateOrderRequest request, Long customerId) {
        return placeOrder(idempotencyKey, request, customerId, null);
    }

    // Same, but with products the caller already fetched (cart checkout fetches the whole cart once).
    public PlacementResult placeOrder(String idempotencyKey, CreateOrderRequest request, Long customerId,
            Map<Long, ProductInfo> preloadedProducts)
    {
        if (customerId == null || customerId <= 0) 
        {
            throw new IllegalArgumentException("An authenticated customer ID is required");
        }
        validateKey(idempotencyKey);
        Map<Long, Integer> quantities = mergeLines(request);
        ShippingAddress address = toAddress(request);
        OrderType type = request.type() == null ? OrderType.PURCHASE : request.type();
        int holdHours = type == OrderType.RESERVATION
                ? (request.holdHours() == null ? defaultHoldHours : request.holdHours()) : 0;
        String requestHash = hash(quantities, address, type, holdHours);

        Optional<Order> existing = orderRepository.findWithItemsByCustomerIdAndIdempotencyKey(customerId,
                idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.get(), requestHash);
        }

        Map<Long, ProductInfo> products = preloadedProducts != null ? preloadedProducts
                : productClient.getProducts(quantities.keySet());
        requireAllAvailable(quantities.keySet(), products);
        BrandInfo brand = requireSingleBrand(quantities.keySet(), products);

        try {
            OrderResponse created = transactionTemplate.execute(status -> persistNewOrder(idempotencyKey,
                    requestHash, quantities, products, brand, address, customerId, type, holdHours));
            return new PlacementResult(created, false);
        } catch (DataIntegrityViolationException e) {
            // Two identical requests raced: the database unique key let exactly one insert win.
            Optional<Order> winner = orderRepository.findWithItemsByCustomerIdAndIdempotencyKey(customerId,
                    idempotencyKey);
            if (winner.isPresent()) {
                return replay(winner.get(), requestHash);
            }
            throw e;
        }
    }

    private OrderResponse persistNewOrder(String idempotencyKey, String requestHash, Map<Long, Integer> quantities,
            Map<Long, ProductInfo> products, BrandInfo brand, ShippingAddress address, Long customerId,
            OrderType type, int holdHours) 
    {

        // Same state as the seller: CGST + SGST. Different state: IGST.
        boolean intraState = PricingEngine.isIntraState(brand.state(), address.getState());

        List<LineInput> inputs = quantities.entrySet().stream().map(line ->
        {
            ProductInfo p = products.get(line.getKey());
            return new LineInput(line.getKey(), line.getValue(), p.price(), p.gstRatePercent(), p.offerTerms());
        }).toList();
        PricedOrder priced = PricingEngine.price(inputs, intraState, shippingPolicy);

        OrderTotals totals = new OrderTotals(priced.subtotal(), priced.discountTotal(), priced.taxableTotal(),
                priced.cgstTotal(), priced.sgstTotal(), priced.igstTotal(), priced.shippingFee());
        SellerSnapshot seller = new SellerSnapshot(brand.name(), brand.legalName(), brand.gstin(), brand.state(),
                brand.city());
        Instant reserveExpiresAt = type == OrderType.RESERVATION ? Instant.now().plus(holdHours, ChronoUnit.HOURS)
                : null;

        Order order = new Order(idempotencyKey, requestHash, customerId, address, brand.id(), brand.sellerId(),
                seller, totals, priced.grandTotal(), type, reserveExpiresAt, currency);
        int index = 0;
        for (Long productId : quantities.keySet())
        {
            ProductInfo p = products.get(productId);
            order.addItem(new OrderItem(productId, p.name(), p.sku(), p.size(), p.color(), p.hsnCode(),
                    priced.lines().get(index++)));
        }
        Order saved = orderRepository.save(order);

        String eventId = UUID.randomUUID().toString();
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        List<OrderCreatedEvent.Item> eventItems = quantities.entrySet().stream()
                .map(e -> new OrderCreatedEvent.Item(e.getKey(), e.getValue()))
                .toList();
        saveOutbox(new OutboxEvent(eventId, saved.getId(), ORDER_CREATED, ORDER_EVENTS_TOPIC,
                toJson(new OrderCreatedEvent(eventId, saved.getId(), eventItems, correlationId, Instant.now())),
                correlationId));

        log.info("{} order {} created in state PENDING (brand {}, total {})", type, saved.getId(), brand.id(),
                priced.grandTotal());
        return OrderResponse.from(saved);
    }

    private void requireAllAvailable(Set<Long> productIds, Map<Long, ProductInfo> products) 
    {
        for (Long productId : productIds)
        {
            ProductInfo product = products.get(productId);
            if (product == null) 
            {
                throw new ProductNotFoundException(productId);
            }
            if (!product.active())
            {
                throw new ProductInactiveException(productId);
            }
        }
    }

    // One order = one seller, because each seller ships and invoices on their own. 
    private BrandInfo requireSingleBrand(Set<Long> productIds, Map<Long, ProductInfo> products)
    {
        Map<Long, BrandInfo> brands = new LinkedHashMap<>();
        for (Long productId : productIds) {
            BrandInfo brand = products.get(productId).brand();
            if (brand == null || brand.id() == null) {
                throw new InvalidRequestException("Product " + productId + " has no seller information");
            }
            brands.putIfAbsent(brand.id(), brand);
        }
        if (brands.size() > 1) {
            throw new InvalidRequestException("An order can only contain products of one brand "
                    + "(this one has " + brands.size() + "). Use cart checkout to buy from several brands at once.");
        }
        return brands.values().iterator().next();
    }

    // reading orders

    @Transactional(readOnly = true)
    public OrderResponse get(Long id)
    {
        return orderRepository.findWithItemsById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public OrderResponse getForCustomer(Long id, Long customerId)
    {
        return orderRepository.findWithItemsByIdAndCustomerId(id, customerId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public OrderResponse getForSeller(Long id, Long sellerId)
    {
        return orderRepository.findWithItemsByIdAndSellerId(id, sellerId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> listForCustomer(Long customerId, int page, int size,
            OrderStatus status) {
        PageRequest pageable = pageable(page, size);
        Page<Order> result = status == null
                ? orderRepository.findByCustomerId(customerId, pageable)
                : orderRepository.findByCustomerIdAndStatus(customerId, status, pageable);
        return PageResponse.of(result.map(OrderSummaryResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> listForSeller(Long sellerId, int page, int size, OrderStatus status)
    {
        PageRequest pageable = pageable(page, size);
        Page<Order> result = status == null
                ? orderRepository.findBySellerId(sellerId, pageable)
                : orderRepository.findBySellerIdAndStatus(sellerId, status, pageable);
        return PageResponse.of(result.map(OrderSummaryResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> list(int page, int size, OrderStatus status)
    {
        PageRequest pageable = pageable(page, size);
        Page<Order> result = status == null
                ? orderRepository.findAll(pageable)
                : orderRepository.findByStatus(status, pageable);
        return PageResponse.of(result.map(OrderSummaryResponse::from));
    }

    @Transactional(readOnly = true)
    public SellerSummaryResponse sellerSummary(Long sellerId)
    {
        Map<OrderStatus, Long> byStatus = new EnumMap<>(OrderStatus.class);
        for (OrderStatus s : OrderStatus.values()) {
            byStatus.put(s, 0L);
        }
        long total = 0;
        BigDecimal sales = BigDecimal.ZERO.setScale(2);
        for (Object[] row : orderRepository.summarizeBySeller(sellerId)) 
        {
            OrderStatus status = (OrderStatus) row[0];
            long count = ((Number) row[1]).longValue();
            byStatus.put(status, count);
            total += count;
            if (status == OrderStatus.CONFIRMED || status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED) {
                sales = sales.add((BigDecimal) row[2]);
            }
        }
        return new SellerSummaryResponse(total, byStatus, sales);
    }

    //cancelling, reserving, confirming

    // CONFIRMED or RESERVED -> CANCELLED, plus an OrderCancelled event so Inventory releases the stock.
    @Transactional
    public OrderResponse cancel(Long orderId) 
    {
        boolean cancelled = orderRepository.transition(orderId, OrderStatus.CONFIRMED.name(), OrderStatus.CANCELLED.name()) == 1
                || orderRepository.transition(orderId, OrderStatus.RESERVED.name(),OrderStatus.CANCELLED.name()) == 1;
        if (cancelled) {
            publishCancelled(orderId);
            log.info("Order {} cancelled", orderId);
            return get(orderId);
        }
        return resolveFailedTransition(orderId, OrderStatus.CANCELLED, "cancelled");
    }

    @Transactional
    public OrderResponse cancelForCustomer(Long orderId, Long customerId)
    {
        // Query by both identifiers so a customer cannot cancel another customer's order.
        orderRepository.findWithItemsByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return cancel(orderId);
    }

    /*
      The customer commits to a reservation: RESERVED -> CONFIRMED. Refused once the hold time has
      run out, even if the expiry job has not marked it EXPIRED yet.
     */
    @Transactional
    public OrderResponse confirmReservation(Long orderId, Long customerId) 
    {
        Order order = orderRepository.findWithItemsByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (order.getOrderType() != OrderType.RESERVATION) 
        {
            throw new InvalidRequestException("Order " + orderId + " is not a reservation");
        }
        if (order.getStatus() == OrderStatus.RESERVED && order.getReserveExpiresAt() != null
                && !Instant.now().isBefore(order.getReserveExpiresAt())) 
        {
            throw new InvalidOrderStateException(orderId, order.getStatus(),
                    "confirmed because its reservation has expired");
        }
        if (orderRepository.transition(orderId, OrderStatus.RESERVED.name(), OrderStatus.CONFIRMED.name()) == 1) {
            log.info("Reservation {} confirmed by the customer", orderId);
            return get(orderId);
        }
        return resolveFailedTransition(orderId, OrderStatus.CONFIRMED, "confirmed");
    }

    //Called by the expiry job: RESERVED -> EXPIRED and release the held stock.
    @Transactional
    public boolean expireReservation(Long orderId) {
        if (orderRepository.transition(orderId, OrderStatus.RESERVED.name(), OrderStatus.EXPIRED.name()) == 1)
        {
            publishCancelled(orderId);
            log.info("Reservation {} expired, stock released", orderId);
            return true;
        }
        return false; // the customer confirmed or cancelled it first
    }

    // Called when Inventory reports the stock was reserved. A reservation order is held, a purchase is confirmed.
    @Transactional
    public OrderResponse markConfirmed(Long orderId)
    {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        OrderStatus target = order.getOrderType() == OrderType.RESERVATION
                ? OrderStatus.RESERVED : OrderStatus.CONFIRMED;
        if (orderRepository.transition(orderId, OrderStatus.PENDING.name(), target.name()) == 1) {
            log.info("Order {} is now {}", orderId, target);
            return get(orderId);
        }
        return resolveFailedTransition(orderId, target, "confirmed");
    }

    // Called when Inventory reports the stock could not be reserved.
    @Transactional
    public OrderResponse markRejected(Long orderId) {
        if (orderRepository.transition(orderId, OrderStatus.PENDING.name(), OrderStatus.REJECTED.name()) == 1) {
            log.info("Order {} rejected", orderId);
            return get(orderId);
        }
        return resolveFailedTransition(orderId, OrderStatus.REJECTED, "rejected");
    }

    //  seller fulfilment

    // The seller hands the parcel to the courier: CONFIRMED -> SHIPPED
    @Transactional
    public OrderResponse ship(Long orderId, Long sellerId, String trackingNumber) 
    {
        orderRepository.findWithItemsByIdAndSellerId(orderId, sellerId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (orderRepository.transition(orderId, OrderStatus.CONFIRMED.name(), OrderStatus.SHIPPED.name()) == 1)
        {
            String tracking = trackingNumber == null || trackingNumber.isBlank() ? null : trackingNumber.trim();
            orderRepository.recordShipment(orderId, tracking, Instant.now());
            log.info("Order {} shipped", orderId);
            return get(orderId);
        }
        return resolveFailedTransition(orderId, OrderStatus.SHIPPED, "shipped");
    }

    // SHIPPED -> DELIVERED.
    @Transactional
    public OrderResponse deliver(Long orderId, Long sellerId) 
    {
        orderRepository.findWithItemsByIdAndSellerId(orderId, sellerId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (orderRepository.transition(orderId, OrderStatus.SHIPPED.name(), OrderStatus.DELIVERED.name()) == 1) {
            orderRepository.recordDelivery(orderId, Instant.now());
            log.info("Order {} delivered", orderId);
            return get(orderId);
        }
        return resolveFailedTransition(orderId, OrderStatus.DELIVERED, "delivered");
    }

    // helpers

    private void publishCancelled(Long orderId) 
    {
        String eventId = UUID.randomUUID().toString();
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        saveOutbox(new OutboxEvent(eventId, orderId, ORDER_CANCELLED, ORDER_EVENTS_TOPIC,
                toJson(new OrderCancelledEvent(eventId, orderId, correlationId, Instant.now())), correlationId));
    }

    private void saveOutbox(OutboxEvent event)
    {
        outboxRepository.save(event);
    }

    private PageRequest pageable(int page, int size) 
    {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    }

    private PlacementResult replay(Order existing, String requestHash)
    {
        // The lookup is already per customer, so only the request content has to match.
        if (!existing.getRequestHash().equals(requestHash))
        {
            throw new IdempotencyKeyConflictException();
        }
        return new PlacementResult(OrderResponse.from(existing), true);
    }

    // The conditional update changed nothing: find out why and answer idempotently or with a clear error.
    private OrderResponse resolveFailedTransition(Long orderId, OrderStatus target, String action) {
        Order current = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (current.getStatus() == target)
        {
            return OrderResponse.from(current);
        }
        throw new InvalidOrderStateException(orderId, current.getStatus(), action);
    }

    private void validateKey(String key) 
    {
        if (key == null || key.isBlank())
        {
            throw new InvalidRequestException("Idempotency-Key header must not be blank");
        }
        if (key.length() > 100) 
        {
            throw new InvalidRequestException("Idempotency-Key must be at most 100 characters");
        }
    }

    // Same product on several lines is merged, and lines are sorted, so equal requests hash equally.
    private Map<Long, Integer> mergeLines(CreateOrderRequest request) 
    {
        Map<Long, Integer> merged = new TreeMap<>();
        for (OrderItemRequest line : request.items()) 
        {
            merged.merge(line.productId(), line.quantity(), Integer::sum);
        }
        return merged;
    }

    // The controller already rejects a missing address; this keeps the rule true for any other caller too.
    private ShippingAddress toAddress(CreateOrderRequest request) 
    {
        ShippingAddressRequest a = request.shippingAddress();
        if (a == null) 
        {
            throw new InvalidRequestException("shippingAddress is required");
        }
        return new ShippingAddress(a.fullName(), a.phone(), a.line1(), a.line2(),
                a.city(), a.state(), a.postalCode(), a.country());
    }

    // The address, order type and hold time are part of the hash: same key with different content is a different request.
    private String hash(Map<Long, Integer> quantities, ShippingAddress address, OrderType type, int holdHours)
    {
        StringBuilder canonical = new StringBuilder();
        quantities.forEach((productId, qty) -> canonical.append(productId).append(':').append(qty).append(';'));
        canonical.append('|').append(address.toCanonicalString());
        canonical.append('|').append(type).append(':').append(holdHours);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) 
        {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private String toJson(Object payload)
    {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize event payload", e);
        }
    }
}
