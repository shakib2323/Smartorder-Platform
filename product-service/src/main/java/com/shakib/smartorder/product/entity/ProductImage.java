package com.shakib.smartorder.product.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Plain id: images are always loaded in batches by product id.
    @Column(name = "product_id", nullable = false, updatable = false)
    private Long productId;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ProductImage() {
        // required by JPA
    }

    public ProductImage(Long productId, String url, int sortOrder, boolean primary) {
        this.productId = productId;
        this.url = url;
        this.sortOrder = sortOrder;
        this.primary = primary;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public void setPrimary(boolean primary) { this.primary = primary; }

    public Long getId() { 
    	return id; 
    }
    public Long getProductId() { 
    	return productId; 
    }
    public String getUrl() { 
    	return url; 
    }
    public int getSortOrder() { 
    	return sortOrder; 
    }
    public boolean isPrimary() { 
    	return primary; 
    }
}
