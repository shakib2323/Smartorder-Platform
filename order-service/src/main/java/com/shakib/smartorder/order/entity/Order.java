package com.shakib.smartorder.order.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.shakib.smartorder.order.enums.OrderStatus;
import com.shakib.smartorder.order.enums.OrderType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "idempotency_key", nullable = false, length = 100, updatable = false)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, columnDefinition = "CHAR(64)", updatable = false)
    private String requestHash;

    @Column(name = "customer_id")
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "order_type", nullable = false, length = 12)
    private OrderType orderType;

    // For reservations: the stock is held until this moment, then released.
    @Column(name = "reserve_expires_at")
    private Instant reserveExpiresAt;

    // Every order belongs to exactly one seller (brand); null only for orders placed before sellers existed.
    @Column(name = "brand_id")
    private Long brandId;

    // The seller's user id, used to show the order to its seller.
    @Column(name = "seller_id")
    private Long sellerId;

    @Embedded
    private SellerSnapshot seller;

    @Embedded
    private OrderTotals totals;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "payment_method", nullable = false, length = 10)
    private String paymentMethod;

    @Column(name = "tracking_number", length = 60)
    private String trackingNumber;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Where to deliver. Copied onto the order, so later changes never touch old orders.
    @Embedded
    private ShippingAddress shippingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
        // required by JPA
    }

    public Order(String idempotencyKey, String requestHash, Long customerId, ShippingAddress shippingAddress,
            Long brandId, Long sellerId, SellerSnapshot seller, OrderTotals totals, BigDecimal totalAmount,
            OrderType orderType, Instant reserveExpiresAt, String currency) {
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.customerId = customerId;
        this.shippingAddress = shippingAddress;
        this.brandId = brandId;
        this.sellerId = sellerId;
        this.seller = seller;
        this.totals = totals;
        this.totalAmount = totalAmount;
        this.orderType = orderType;
        this.reserveExpiresAt = reserveExpiresAt;
        this.currency = currency;
        this.paymentMethod = "COD";
        this.status = OrderStatus.PENDING;
    }

    public void addItem(OrderItem item) {
        item.attachTo(this);
        items.add(item);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { 
    	return id; 
    }
    public Long getCustomerId() { 
    	return customerId; 
    }
    public OrderStatus getStatus() { 
    	return status; 
    }
    public BigDecimal getTotalAmount() { 
     return totalAmount; 
    }
    public String getIdempotencyKey() { 
    	return idempotencyKey; 
    }
    public String getRequestHash() { 
    	return requestHash; 
    }
    public Instant getCreatedAt() { 
    	return createdAt; 
    }
    public ShippingAddress getShippingAddress() { 
    	return shippingAddress; 
    }
    public OrderType getOrderType() { 
    	return orderType; 
    }
    public Instant getReserveExpiresAt() { 
    	return reserveExpiresAt; 
    }
    public Long getBrandId() { 
    	return brandId; 
    }
    public Long getSellerId() { 
    	return sellerId; 
    }
    public SellerSnapshot getSeller() { 
    	return seller; 
    }
    public OrderTotals getTotals() { 
    	return totals; 
    }
    public String getCurrency() { 
    	return currency; 
    }
    public String getPaymentMethod() { 
    	return paymentMethod; 
    }
    public String getTrackingNumber() { 
    	return trackingNumber; 
    }
    public Instant getShippedAt() { 
    	return shippedAt; 
    }
    public Instant getDeliveredAt() { 
    	return deliveredAt; 
    }
    public List<OrderItem> getItems() { 
    	return Collections.unmodifiableList(items); 
    }
}

