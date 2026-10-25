package com.shakib.smartorder.order.dto;

import com.shakib.smartorder.order.entity.ShippingAddress;

public record ShippingAddressResponse(
        String fullName,
        String phone,
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country) {

    public static ShippingAddressResponse from(ShippingAddress a) {
        if (a == null) {
            return null;
        }
        return new ShippingAddressResponse(a.getFullName(), a.getPhone(), a.getLine1(), a.getLine2(),
                a.getCity(), a.getState(), a.getPostalCode(), a.getCountry());
    }
}
