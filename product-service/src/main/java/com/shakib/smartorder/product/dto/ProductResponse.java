package com.shakib.smartorder.product.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.shakib.smartorder.product.entity.Offer;
import com.shakib.smartorder.product.entity.Product;
import com.shakib.smartorder.product.entity.ProductImage;
import com.shakib.smartorder.product.enums.ProductCategory;
import com.shakib.smartorder.product.service.OfferPricing;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        BigDecimal discountedPrice,
        OfferResponse appliedOffer,
        List<OfferResponse> offers,
        boolean active,
        ProductCategory category,
        String size,
        String color,
        String sku,
        BigDecimal gstRatePercent,
        String hsnCode,
        String imageUrl,
        List<ImageResponse> images,
        BrandSummary brand,
        Instant createdAt) {

    /*
      @param activeOffers offers of the product's brand that are on and inside their date window
                          (the caller loaded them); the ones that cover this product are picked here.
     */
    public static ProductResponse from(Product p, List<ProductImage> images, List<Offer> activeOffers) 
    {
        List<Offer> forThisProduct = activeOffers.stream()
                .filter(o -> o.appliesTo(p.getId(), o.getMinQuantity()))
                .toList();
        Offer best = OfferPricing.bestOffer(p.getId(), p.getPrice(), 1, forThisProduct);
        BigDecimal discounted = best == null ? p.getPrice() : p.getPrice().subtract(best.discountPerUnit(p.getPrice()));

        List<ImageResponse> imageResponses = images.stream().map(ImageResponse::from).toList();
        String primaryUrl = images.stream().filter(ProductImage::isPrimary).map(ProductImage::getUrl).findFirst()
                .orElse(images.isEmpty() ? null : images.get(0).getUrl());

        return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(), discounted,
                best == null ? null : OfferResponse.from(best),
                forThisProduct.stream().map(OfferResponse::from).toList(),
                p.isActive(), p.getCategory(), p.getSize(), p.getColor(), p.getSku(), p.getGstRatePercent(),
                p.getHsnCode(), primaryUrl, imageResponses, BrandSummary.from(p.getBrand()), p.getCreatedAt());
    }
}
