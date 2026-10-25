package com.shakib.smartorder.inventory.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    // The seller who owns this stock (user id). 0 = platform, null = created before sellers existed.
    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Inventory() {
        // required by JPA
    }

    public Inventory(Long productId, int availableQuantity) {
        this(productId, null, availableQuantity);
    }

    public Inventory(Long productId, Long sellerId, int availableQuantity) {
        this.productId = productId;
        this.sellerId = sellerId;
        this.availableQuantity = availableQuantity;
    }

    public void assignSeller(Long sellerId) { this.sellerId = sellerId; }

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { 
    	return id; 
    }
    public Long getProductId() { 
    	return productId; 
    }
    public int getAvailableQuantity() { 
    	return availableQuantity; 
    }
    public Long getSellerId() { 
    	return sellerId; 
    }
    public Instant getUpdatedAt() { 
    	return updatedAt; 
    }
}

