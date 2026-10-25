package com.shakib.smartorder.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/*
  Who sold the goods, copied from the brand when the order was placed. An invoice must keep showing
  the details that were true on that day, even if the seller edits their brand later.
 */
@Embeddable
public class SellerSnapshot {

    @Column(name = "brand_name", length = 120)
    private String brandName;

    @Column(name = "seller_legal_name", length = 200)
    private String legalName;

    @Column(name = "seller_gstin", length = 15)
    private String gstin;

    @Column(name = "seller_state", length = 100)
    private String state;

    @Column(name = "seller_city", length = 100)
    private String city;

    protected SellerSnapshot() {
        // required by JPA
    }

    public SellerSnapshot(String brandName, String legalName, String gstin, String state, String city) {
        this.brandName = brandName;
        this.legalName = legalName;
        this.gstin = gstin;
        this.state = state;
        this.city = city;
    }

    public String getBrandName() { return brandName; }
    public String getLegalName() { return legalName; }
    public String getGstin() { return gstin; }
    public String getState() { return state; }
    public String getCity() { return city; }
}
