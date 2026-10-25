package com.shakib.smartorder.product.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.shakib.smartorder.product.enums.DiscountType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/*
  A seller's discount. productId == null applies it to every product of the brand.
  minQuantity lets a seller run "buy 3 or more, get 10% off" style offers.
 */
@Entity
@Table(name = "offers")
public class Offer {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "brand_id", nullable = false, updatable = false)
    private Long brandId;

    @Column(name = "product_id", updatable = false)
    private Long productId;

    @Column(nullable = false, length = 120)
    private String title;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "discount_type", nullable = false, length = 10)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_quantity", nullable = false)
    private int minQuantity;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Offer() {
        // required by JPA
    }

    public Offer(Long brandId, Long productId, String title, DiscountType discountType, BigDecimal discountValue,
            int minQuantity, Instant startsAt, Instant endsAt) {
        this.brandId = brandId;
        this.productId = productId;
        this.title = title;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minQuantity = minQuantity;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.active = true;
    }

    // Does this offer cover the product, and is the quantity high enough? (The time window is checked by the query.) 
    public boolean appliesTo(Long forProductId, int quantity) {
        boolean productMatches = productId == null || productId.equals(forProductId);
        return active && productMatches && quantity >= minQuantity;
    }

    // Discount for ONE unit, never more than the unit price, so a price can never go negative. 
    public BigDecimal discountPerUnit(BigDecimal unitPrice) {
        BigDecimal raw = discountType == DiscountType.PERCENT
                ? unitPrice.multiply(discountValue).divide(HUNDRED, 2, RoundingMode.HALF_UP)
                : discountValue;
        return raw.min(unitPrice).setScale(2, RoundingMode.HALF_UP);
    }

    public void deactivate() { this.active = false; }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public Long getId() { 
    	return id; 
    }
    public Long getBrandId() { 
    	return brandId; 
    }
    public Long getProductId() { 
    	return productId; 
    }
    public String getTitle() { 
    	return title; 
    }
    public DiscountType getDiscountType() { 
    	return discountType; 
    }
    public BigDecimal getDiscountValue() { 
    	return discountValue; 
    }
    public int getMinQuantity() { 
    	return minQuantity; 
    }
    public Instant getStartsAt() { 
    	return startsAt; 
    }
    public Instant getEndsAt() { 
    	return endsAt; 
    }
    public boolean isActive() { 
    	return active; 
    }
}
