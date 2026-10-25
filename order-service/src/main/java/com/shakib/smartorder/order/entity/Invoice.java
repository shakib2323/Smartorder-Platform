package com.shakib.smartorder.order.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

// The invoice number and issue date of an order. The content is always rebuilt from the order snapshot.
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private Long orderId;

    @Column(name = "invoice_number", nullable = false, length = 30, updatable = false)
    private String invoiceNumber;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    protected Invoice() {
        // required by JPA
    }

    public Invoice(Long orderId, String invoiceNumber) {
        this.orderId = orderId;
        this.invoiceNumber = invoiceNumber;
    }

    @PrePersist
    void onCreate() {
        this.issuedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public Instant getIssuedAt() { return issuedAt; }
}
