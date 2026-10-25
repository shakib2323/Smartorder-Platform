package com.shakib.smartorder.product.dto;

import com.shakib.smartorder.product.entity.Brand;

// What shoppers see: no legal or contact details.
public record BrandResponse(Long id, String name, String description, String logoUrl, String city, String state) {

    public static BrandResponse from(Brand b) {
        return new BrandResponse(b.getId(), b.getName(), b.getDescription(), b.getLogoUrl(), b.getCity(),
                b.getState());
    }
}
