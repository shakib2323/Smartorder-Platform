package com.shakib.smartorder.product.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.shakib.smartorder.product.enums.ProductCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false, updatable = false)
    private Brand brand;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    // Selling price per unit. GST is already included in it (Indian MRP style).
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private ProductCategory category;

    @Column(length = 20)
    private String size;

    @Column(length = 30)
    private String color;

    @Column(length = 50)
    private String sku;

    @Column(name = "gst_rate_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal gstRatePercent;

    @Column(name = "hsn_code", length = 8)
    private String hsnCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Product() {
        // required by JPA
    }

    public Product(Brand brand, String name, String description, BigDecimal price, ProductCategory category,
            String size, String color, String sku, BigDecimal gstRatePercent, String hsnCode) {
        this.brand = brand;
        this.active = true;
        update(name, description, price, category, size, color, sku, gstRatePercent, hsnCode);
    }

    public void update(String name, String description, BigDecimal price, ProductCategory category,
            String size, String color, String sku, BigDecimal gstRatePercent, String hsnCode) {
        this.name = name.trim();
        this.description = blankToNull(description);
        this.price = price;
        this.category = category;
        this.size = blankToNull(size);
        this.color = blankToNull(color);
        this.sku = blankToNull(sku);
        this.gstRatePercent = gstRatePercent == null ? BigDecimal.ZERO.setScale(2) : gstRatePercent;
        this.hsnCode = blankToNull(hsnCode);
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    public void deactivate() { this.active = false; }
    public void activate() { this.active = true; }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { 
    	return id; 
    }
    public Brand getBrand() { 
    	return brand; 
    }
    public String getName() { 
    	return name; 
    }
    public String getDescription() {
    	return description; 
    }
    public BigDecimal getPrice() { 
    	return price; 
    }
    public boolean isActive() { 
    	return active; 
    }
    public ProductCategory getCategory() { 
    	return category; 
    }
    public String getSize() { 
    	return size; 
    }
    public String getColor() { 
    	return color; 
    }
    public String getSku() { 
    	return sku; 
    }
    public BigDecimal getGstRatePercent() { 
    	return gstRatePercent; 
    }
    public String getHsnCode() { 
    	return hsnCode; 
    }
    public Instant getCreatedAt() { 
    	return createdAt; 
    }
}
