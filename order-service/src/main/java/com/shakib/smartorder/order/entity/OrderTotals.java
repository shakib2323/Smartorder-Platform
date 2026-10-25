package com.shakib.smartorder.order.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

// The money breakdown of an order, stored once at placement. totalAmount on the order is what the customer pays. 
@Embeddable
public class OrderTotals {

    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal discountTotal;

    @Column(name = "taxable_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxableTotal;

    @Column(name = "cgst_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal cgstTotal;

    @Column(name = "sgst_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal sgstTotal;

    @Column(name = "igst_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal igstTotal;

    @Column(name = "shipping_fee", nullable = false, precision = 14, scale = 2)
    private BigDecimal shippingFee;

    protected OrderTotals() {
        // required by JPA
    }

    public OrderTotals(BigDecimal subtotal, BigDecimal discountTotal, BigDecimal taxableTotal, BigDecimal cgstTotal,
            BigDecimal sgstTotal, BigDecimal igstTotal, BigDecimal shippingFee) {
        this.subtotal = subtotal;
        this.discountTotal = discountTotal;
        this.taxableTotal = taxableTotal;
        this.cgstTotal = cgstTotal;
        this.sgstTotal = sgstTotal;
        this.igstTotal = igstTotal;
        this.shippingFee = shippingFee;
    }

    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscountTotal() { return discountTotal; }
    public BigDecimal getTaxableTotal() { return taxableTotal; }
    public BigDecimal getCgstTotal() { return cgstTotal; }
    public BigDecimal getSgstTotal() { return sgstTotal; }
    public BigDecimal getIgstTotal() { return igstTotal; }
    public BigDecimal getShippingFee() { return shippingFee; }
}
