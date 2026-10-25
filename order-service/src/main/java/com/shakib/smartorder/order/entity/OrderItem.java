package com.shakib.smartorder.order.entity;

import java.math.BigDecimal;

import com.shakib.smartorder.order.billing.PricedLine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Plain id, not a relationship: the product lives in another service's database.
    @Column(name = "product_id", nullable = false)
    private Long productId;

    // Snapshots taken at order time, so later product edits never change old orders or invoices.
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(length = 50)
    private String sku;

    @Column(length = 20)
    private String size;

    @Column(length = 30)
    private String color;

    @Column(name = "hsn_code", length = 8)
    private String hsnCode;

    @Column(nullable = false)
    private int quantity;

    // List price per unit (GST included), before the seller's offer.
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "gst_rate_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal gstRatePercent;

    // Total discount on this line, and the offer that gave it.
    @Column(name = "discount_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "offer_title", length = 120)
    private String offerTitle;

    // (unitPrice - discount per unit) x quantity, GST included. This is what the customer pays for the line.
    @Column(name = "line_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "taxable_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxableValue;

    @Column(name = "cgst_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal cgstAmount;

    @Column(name = "sgst_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal sgstAmount;

    @Column(name = "igst_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal igstAmount;

    protected OrderItem() {
        // required by JPA
    }

    public OrderItem(Long productId, String productName, String sku, String size, String color, String hsnCode,
            PricedLine priced) {
        this.productId = productId;
        this.productName = productName;
        this.sku = sku;
        this.size = size;
        this.color = color;
        this.hsnCode = hsnCode;
        this.quantity = priced.quantity();
        this.unitPrice = priced.listPrice();
        this.gstRatePercent = priced.gstRatePercent();
        this.discountAmount = priced.discountAmount();
        this.offerTitle = priced.offerTitle();
        this.lineTotal = priced.lineTotal();
        this.taxableValue = priced.taxableValue();
        this.cgstAmount = priced.cgst();
        this.sgstAmount = priced.sgst();
        this.igstAmount = priced.igst();
    }

    void attachTo(Order order) {
        this.order = order;
    }

    public Long getId() { 
    	return id; 
    }
    public Long getProductId() { 
    	return productId; 
    }
    public String getProductName() { 
    	return productName; 
    }
    public String getSku() { 
    	return sku; 
    }
    public String getSize() { 
    	return size; 
    }
    public String getColor() { 
    	return color; 
    }
    public String getHsnCode() { 
    	return hsnCode; 
    }
    public int getQuantity() { 
    	return quantity; 
    }
    public BigDecimal getUnitPrice() { 
    	return unitPrice; 
    }
    public BigDecimal getGstRatePercent() { 
    	return gstRatePercent; 
    }
    public BigDecimal getDiscountAmount() { 
    	return discountAmount; 
    }
    public String getOfferTitle() { 
    	return offerTitle; 
    }
    public BigDecimal getLineTotal() { 
    	return lineTotal; 
    }
    public BigDecimal getTaxableValue() { 
    	return taxableValue; 
    }
    public BigDecimal getCgstAmount() { 
    	return cgstAmount; 
    }
    public BigDecimal getSgstAmount() { 
    	return sgstAmount; 
    }
    public BigDecimal getIgstAmount() { 
    	return igstAmount; 
    }
}
