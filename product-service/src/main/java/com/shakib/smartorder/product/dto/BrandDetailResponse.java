package com.shakib.smartorder.product.dto;

import java.time.Instant;

import com.shakib.smartorder.product.entity.Brand;
import com.shakib.smartorder.product.enums.BrandStatus;

// For the brand owner and admins: includes legal details and the approval status.
public record BrandDetailResponse(
        Long id,
        Long ownerUserId,
        String name,
        String legalName,
        String description,
        String logoUrl,
        String contactEmail,
        String gstin,
        String city,
        String state,
        BrandStatus status,
        Instant createdAt) {

    public static BrandDetailResponse from(Brand b) 
    {
        return new BrandDetailResponse(b.getId(), b.getOwnerUserId(), b.getName(), b.getLegalName(),
                b.getDescription(), b.getLogoUrl(), b.getContactEmail(), b.getGstin(), b.getCity(), b.getState(),
                b.getStatus(), b.getCreatedAt());
    }
}
