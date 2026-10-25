package com.shakib.smartorder.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

// Stored as columns on the orders table. A snapshot: it never changes after the order is placed. 
@Embeddable
public class ShippingAddress {

    @Column(name = "shipping_full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "shipping_phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "shipping_line1", nullable = false, length = 200)
    private String line1;

    @Column(name = "shipping_line2", length = 200)
    private String line2;

    @Column(name = "shipping_city", nullable = false, length = 100)
    private String city;

    @Column(name = "shipping_state", nullable = false, length = 100)
    private String state;

    @Column(name = "shipping_postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(name = "shipping_country", nullable = false, length = 100)
    private String country;

    protected ShippingAddress() {
        // required by JPA
    }

    public ShippingAddress(String fullName, String phone, String line1, String line2,
            String city, String state, String postalCode, String country) {
        this.fullName = clean(fullName);
        this.phone = clean(phone);
        this.line1 = clean(line1);
        this.line2 = clean(line2);
        this.city = clean(city);
        this.state = clean(state);
        this.postalCode = clean(postalCode);
        this.country = clean(country);
    }

    /**
     * One string that is identical for identical addresses. Each part is length-prefixed so
     * two different addresses can never produce the same text. Used for the idempotency hash.
     */
    public String toCanonicalString() {
        String[] parts = { fullName, phone, line1, line2 == null ? "" : line2, city, state, postalCode, country };
        StringBuilder text = new StringBuilder();
        for (String part : parts) {
            text.append(part.length()).append(':').append(part).append(';');
        }
        return text.toString();
    }

    // trims spaces; an empty optional value is stored as null
    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getLine1() { return line1; }
    public String getLine2() { return line2; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPostalCode() { return postalCode; }
    public String getCountry() { return country; }
}
