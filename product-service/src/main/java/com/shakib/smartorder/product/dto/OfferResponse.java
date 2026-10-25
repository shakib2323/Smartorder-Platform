package com.shakib.smartorder.product.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.shakib.smartorder.product.entity.Offer;
import com.shakib.smartorder.product.enums.DiscountType;

public record OfferResponse(
        Long id,
        Long productId,
        String title,
        DiscountType discountType,
        BigDecimal discountValue,
        int minQuantity,
        Instant startsAt,
        Instant endsAt,
        boolean active) {

    public static OfferResponse from(Offer o) {
        return new OfferResponse(o.getId(), o.getProductId(), o.getTitle(), o.getDiscountType(),
                o.getDiscountValue(), o.getMinQuantity(), o.getStartsAt(), o.getEndsAt(), o.isActive());
    }
}
