package com.shakib.smartorder.product.dto;

import com.shakib.smartorder.product.entity.Brand;

/*
  The sold by block of a product. The Order service snapshots it onto the invoice, which is why
  it carries the legal name, GSTIN and state (GSTIN is public information in India). sellerId is the
  owner's user id, which lets the Order service show an order to the seller it belongs to.
 */
public record BrandSummary(Long id, String name, String legalName, String gstin, String state, String city,
        Long sellerId) {

    public static BrandSummary from(Brand b) {
        return new BrandSummary(b.getId(), b.getName(), b.getLegalName(), b.getGstin(), b.getState(), b.getCity(),
                b.getOwnerUserId());
    }
}
