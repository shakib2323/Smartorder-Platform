package com.shakib.smartorder.product.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.shakib.smartorder.product.enums.BrandStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

// A seller's store. Owner id 0 is the built-in platform brand. New brands wait for admin approval. 
@Entity
@Table(name = "brands")
public class Brand {

    public static final long PLATFORM_OWNER_ID = 0L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false, updatable = false)
    private Long ownerUserId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "legal_name", nullable = false, length = 200)
    private String legalName;

    @Column(length = 1000)
    private String description;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "contact_email", nullable = false, length = 254)
    private String contactEmail;

    @Column(length = 15)
    private String gstin;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private BrandStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Brand() {
        // required by JPA
    }

    public Brand(Long ownerUserId, String name, String legalName, String description, String contactEmail,
            String gstin, String city, String state) {
        this.ownerUserId = ownerUserId;
        this.status = BrandStatus.PENDING;
        update(name, legalName, description, contactEmail, gstin, city, state);
    }

    public void update(String name, String legalName, String description, String contactEmail,
            String gstin, String city, String state) {
        this.name = name.trim();
        this.legalName = legalName.trim();
        this.description = blankToNull(description);
        this.contactEmail = contactEmail.trim();
        this.gstin = gstin == null || gstin.isBlank() ? null : gstin.trim().toUpperCase();
        this.city = city.trim();
        this.state = state.trim();
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    public void approve() { this.status = BrandStatus.APPROVED; }
    public void suspend() { this.status = BrandStatus.SUSPENDED; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public boolean isApproved() { return status == BrandStatus.APPROVED; }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getOwnerUserId() { return ownerUserId; }
    public String getName() { return name; }
    public String getLegalName() { return legalName; }
    public String getDescription() { return description; }
    public String getLogoUrl() { return logoUrl; }
    public String getContactEmail() { return contactEmail; }
    public String getGstin() { return gstin; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public BrandStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
