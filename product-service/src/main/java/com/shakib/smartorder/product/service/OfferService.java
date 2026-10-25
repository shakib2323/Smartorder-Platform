package com.shakib.smartorder.product.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shakib.smartorder.product.dto.OfferRequest;
import com.shakib.smartorder.product.dto.OfferResponse;
import com.shakib.smartorder.product.entity.Brand;
import com.shakib.smartorder.product.entity.Offer;
import com.shakib.smartorder.product.entity.Product;
import com.shakib.smartorder.product.enums.DiscountType;
import com.shakib.smartorder.product.exception.ApiException;
import com.shakib.smartorder.product.repository.BrandRepository;
import com.shakib.smartorder.product.repository.OfferRepository;
import com.shakib.smartorder.product.repository.ProductRepository;
import com.shakib.smartorder.product.security.CurrentUser;

@Service
public class OfferService {

    private static final BigDecimal MAX_PERCENT = new BigDecimal("90");

    private final OfferRepository offerRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    public OfferService(OfferRepository offerRepository, BrandRepository brandRepository,
            ProductRepository productRepository)
    {
        this.offerRepository = offerRepository;
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OfferResponse create(CurrentUser user, OfferRequest request) 
    {
        Brand brand = requireBrand(user);
        validate(request);

        if (request.productId() != null) {
            Product product = productRepository.findById(request.productId())
                    .orElseThrow(() -> ApiException.badRequest("Product " + request.productId() + " does not exist"));
            if (!product.getBrand().getId().equals(brand.getId())) {
                throw ApiException.badRequest("You can only create offers for your own products");
            }
            if (request.discountType() == DiscountType.FLAT
                    && request.discountValue().compareTo(product.getPrice()) >= 0) 
            {
                throw ApiException.badRequest("A flat discount must be less than the product price");
            }
        }

        int minQuantity = request.minQuantity() == null ? 1 : request.minQuantity();
        Offer saved = offerRepository.save(new Offer(brand.getId(), request.productId(), request.title().trim(),
                request.discountType(), request.discountValue(), minQuantity, request.startsAt(), request.endsAt()));
        return OfferResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> listMine(CurrentUser user)
    {
        Brand brand = requireBrand(user);
        return offerRepository.findByBrandIdOrderByIdDesc(brand.getId()).stream().map(OfferResponse::from).toList();
    }

    @Transactional
    public void deactivate(CurrentUser user, Long offerId)
    {
        Brand brand = requireBrand(user);
        Offer offer = offerRepository.findById(offerId)
                .filter(o -> o.getBrandId().equals(brand.getId()))
                .orElseThrow(() -> ApiException.notFound("Offer not found with id " + offerId));
        offer.deactivate();
    }

    private void validate(OfferRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) 
        {
            throw ApiException.badRequest("endsAt must be after startsAt");
        }
        if (!request.endsAt().isAfter(Instant.now())) {
            throw ApiException.badRequest("endsAt must be in the future");
        }
        if (request.discountType() == DiscountType.PERCENT && request.discountValue().compareTo(MAX_PERCENT) > 0) {
            throw ApiException.badRequest("A percentage discount can be at most 90");
        }
    }

    private Brand requireBrand(CurrentUser user)
    {
        return brandRepository.findByOwnerUserId(user.id())
                .orElseThrow(() -> ApiException.conflict("Register your brand before creating offers"));
    }
}
