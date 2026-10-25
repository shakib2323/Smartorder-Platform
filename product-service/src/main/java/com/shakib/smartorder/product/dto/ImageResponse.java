package com.shakib.smartorder.product.dto;

import com.shakib.smartorder.product.entity.ProductImage;

public record ImageResponse(Long id, String url, boolean primary, int sortOrder) {

    public static ImageResponse from(ProductImage i) {
        return new ImageResponse(i.getId(), i.getUrl(), i.isPrimary(), i.getSortOrder());
    }
}
