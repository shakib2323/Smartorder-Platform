package com.shakib.smartorder.order.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shakib.smartorder.order.billing.OfferTerms;

/** The part of the Product service's response that Order cares about. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductInfo(
        Long id,
        String name,
        BigDecimal price,
        boolean active,
        String size,
        String color,
        String sku,
        BigDecimal gstRatePercent,
        String hsnCode,
        BrandInfo brand,
        List<OfferInfo> offers) {

    public ProductInfo {
        gstRatePercent = gstRatePercent == null ? BigDecimal.ZERO : gstRatePercent;
        offers = offers == null ? List.of() : List.copyOf(offers);
    }

    /** A product with no variants, tax or offers (handy for tests). */
    public static ProductInfo basic(Long id, String name, BigDecimal price, boolean active) {
        return new ProductInfo(id, name, price, active, null, null, null, BigDecimal.ZERO, null, null, List.of());
    }

    public List<OfferTerms> offerTerms() {
        return offers.stream().map(OfferInfo::toTerms).toList();
    }

    /** The "sold by" block. Also what the invoice prints about the seller. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BrandInfo(Long id, String name, String legalName, String gstin, String state, String city,
            Long sellerId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OfferInfo(Long id, Long productId, String title, String discountType, BigDecimal discountValue,
            int minQuantity) {

        public OfferTerms toTerms() {
            return new OfferTerms(title, OfferTerms.Type.valueOf(discountType), discountValue, minQuantity,
                    productId);
        }
    }
}
