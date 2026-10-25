package com.shakib.smartorder.order.cart.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "cart_items", uniqueConstraints = @UniqueConstraint(
        name = "uq_cart_customer_product", columnNames = {"customer_id", "product_id"}))
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CartItem() { }

    public CartItem(Long customerId, Long productId, int quantity) 
    {
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
    }

    public void setQuantity(int quantity) { 
    	this.quantity = quantity; 
    }
    public Long getId() { 
    	return id; 
    }
    public Long getCustomerId() { 
    	return customerId; 
    }
    public Long getProductId() { 
    	return productId; 
    }
    public int getQuantity() { 
    	return quantity; 
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { 
    	updatedAt = Instant.now(); 
    }
}
