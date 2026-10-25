package com.shakib.smartorder.product.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shakib.smartorder.product.dto.PageResponse;
import com.shakib.smartorder.product.dto.ProductRequest;
import com.shakib.smartorder.product.dto.ProductResponse;
import com.shakib.smartorder.product.entity.Brand;
import com.shakib.smartorder.product.entity.Offer;
import com.shakib.smartorder.product.entity.OutboxEvent;
import com.shakib.smartorder.product.entity.Product;
import com.shakib.smartorder.product.entity.ProductImage;
import com.shakib.smartorder.product.exception.ApiException;
import com.shakib.smartorder.product.exception.ProductNotFoundException;
import com.shakib.smartorder.product.filter.CorrelationIdFilter;
import com.shakib.smartorder.product.messaging.ProductCreatedEvent;
import com.shakib.smartorder.product.messaging.Topics;
import com.shakib.smartorder.product.repository.BrandRepository;
import com.shakib.smartorder.product.repository.OfferRepository;
import com.shakib.smartorder.product.repository.OutboxEventRepository;
import com.shakib.smartorder.product.repository.ProductImageRepository;
import com.shakib.smartorder.product.repository.ProductRepository;
import com.shakib.smartorder.product.security.CurrentUser;

@Service
public class ProductService {

    public static final String PRODUCT_CREATED = "ProductCreated";

    // The GST slabs a seller may choose from.
    private static final List<BigDecimal> GST_SLABS = List.of(
            new BigDecimal("0"), new BigDecimal("5"), new BigDecimal("12"), new BigDecimal("18"),new BigDecimal("28"));

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final ProductImageRepository imageRepository;
    private final OfferRepository offerRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public ProductService(ProductRepository productRepository, BrandRepository brandRepository,
            ProductImageRepository imageRepository, OfferRepository offerRepository,
            OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.imageRepository = imageRepository;
        this.offerRepository = offerRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    // A seller adds a product to their own (approved) brand; an admin adds to the platform brand. 
    @Transactional
    public ProductResponse create(CurrentUser user, ProductRequest request)
    {
        Brand brand = brandForNewProduct(user);
        BigDecimal gst = validatedGst(request.gstRatePercent());

        Product saved = productRepository.save(new Product(brand, request.name(), request.description(),
                request.price(), request.category(), request.size(), request.color(), request.sku(), gst,
                request.hsnCode()));

        // Same transaction as the product: the event exists if and only if the product does.
        int initialStock = request.initialStock() == null ? 0 : request.initialStock();
        String eventId = UUID.randomUUID().toString();
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        outboxRepository.save(new OutboxEvent(eventId, saved.getId(), PRODUCT_CREATED, Topics.PRODUCT_EVENTS,
                toJson(new ProductCreatedEvent(eventId, saved.getId(), brand.getOwnerUserId(), initialStock,
                        correlationId, Instant.now())),
                correlationId));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(ProductFilter filter, int page, int size)
    {
        List<Specification<Product>> parts = new ArrayList<>();
        parts.add(ProductSpecifications.publiclyVisible());
        if (filter.text() != null && !filter.text().isBlank()) {
            parts.add(ProductSpecifications.nameContains(filter.text()));
        }
        if (filter.category() != null) {
            parts.add(ProductSpecifications.inCategory(filter.category()));
        }
        if (filter.brandId() != null) {
            parts.add(ProductSpecifications.ofBrand(filter.brandId()));
        }
        if (filter.minPrice() != null) {
            parts.add(ProductSpecifications.priceAtLeast(filter.minPrice()));
        }
        if (filter.maxPrice() != null) {
            parts.add(ProductSpecifications.priceAtMost(filter.maxPrice()));
        }
        if (filter.size() != null && !filter.size().isBlank()) {
            parts.add(ProductSpecifications.hasSize(filter.size()));
        }
        if (filter.color() != null && !filter.color().isBlank()) {
            parts.add(ProductSpecifications.hasColor(filter.color()));
        }
        return page(combine(parts), page, size, sortFor(filter.sort()));
    }

    // A seller's own catalogue, including inactive products.
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> mine(CurrentUser user, int page, int size) {
        return page(ProductSpecifications.ownedBy(user.id()), page, size, Sort.by(Sort.Direction.DESC, "id"));
    }

    /*
      Several products in one call (used by Cart and Order so N products cost one request, not N).
      Products that do not exist or are not visible are simply left out of the answer.
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> batch(List<Long> ids)
    {
        Set<Long> unique = new LinkedHashSet<>(ids);
        if (unique.size() > 50) {
            throw ApiException.badRequest("At most 50 ids per request");
        }
        if (unique.isEmpty()) {
            return List.of();
        }
        Specification<Product> spec = ProductSpecifications.publiclyVisible().and(ProductSpecifications.idIn(unique));
        return toResponses(productRepository.findAll(spec, Pageable.unpaged()).getContent());
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        Product product = productRepository.findWithBrandById(id).orElseThrow(() -> new ProductNotFoundException(id));
        if (!product.isActive() || !product.getBrand().isApproved()) 
        {
            throw new ProductNotFoundException(id);
        }
        return toResponse(product);
    }

    @Transactional
    public ProductResponse update(Long id, CurrentUser user, ProductRequest request)
    {
        Product product = requireOwned(id, user);
        product.update(request.name(), request.description(), request.price(), request.category(),
                request.size(), request.color(), request.sku(), validatedGst(request.gstRatePercent()),
                request.hsnCode());
        productRepository.flush();
        return toResponse(product);
    }

    @Transactional
    public void deactivate(Long id, CurrentUser user)
    {
        requireOwned(id, user).deactivate();
    }

    @Transactional
    public void activate(Long id, CurrentUser user)
    {
        requireOwned(id, user).activate();
    }

    // Loads a product and checks the caller may change it: its brand's owner, or an admin.
    @Transactional(readOnly = true)
    public Product requireOwned(Long id, CurrentUser user) 
    {
        Product product = productRepository.findWithBrandById(id).orElseThrow(() -> new ProductNotFoundException(id));
        if (!user.admin() && !product.getBrand().getOwnerUserId().equals(user.id())) {
            throw ApiException.forbidden("You can only change products of your own brand");
        }
        return product;
    }

    // helpers

    private Brand brandForNewProduct(CurrentUser user)
    {
        if (user.admin()) 
        {
            return brandRepository.findByOwnerUserId(Brand.PLATFORM_OWNER_ID)
                    .orElseThrow(() -> new IllegalStateException("The platform brand is missing"));
        }
        Brand brand = brandRepository.findByOwnerUserId(user.id())
                .orElseThrow(() -> ApiException.conflict("Register your brand before adding products"));
        if (!brand.isApproved()) 
        {
            throw ApiException.forbidden("Your brand is " + brand.getStatus()
                    + ". Products can be added once an admin approves it.");
        }
        return brand;
    }

    private BigDecimal validatedGst(BigDecimal requested)
    {
        if (requested == null) 
        {
            return new BigDecimal("0.00");
        }
        boolean allowed = GST_SLABS.stream().anyMatch(slab -> slab.compareTo(requested) == 0);
        if (!allowed) 
        {
            throw ApiException.badRequest("gstRatePercent must be one of 0, 5, 12, 18, 28");
        }
        return requested.setScale(2);
    }

    private PageResponse<ProductResponse> page(Specification<Product> spec, int page, int size, Sort sort) 
    {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sort);
        Page<Product> result = productRepository.findAll(spec, pageable);
        return PageResponse.of(result, toResponses(result.getContent()));
    }

    private Specification<Product> combine(List<Specification<Product>> parts) 
    {
        Specification<Product> all = parts.get(0);
        for (int i = 1; i < parts.size(); i++)
        {
            all = all.and(parts.get(i));
        }
        return all;
    }

    private Sort sortFor(String sort)
    {
        if (sort == null) 
        {
            return Sort.by(Sort.Direction.DESC, "id");
        }
        return switch (sort) 
        		{
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price", "id");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price", "id");
            case "name" -> Sort.by(Sort.Direction.ASC, "name", "id");
            default -> Sort.by(Sort.Direction.DESC, "id");
        };
    }

    private ProductResponse toResponse(Product product) 
    {
        return toResponses(List.of(product)).get(0);
    }

    // Images and offers for the whole page are loaded with one query each, not one per product.
    private List<ProductResponse> toResponses(List<Product> products) 
    {
        if (products.isEmpty()) 
        {
            return List.of();
        }
        List<Long> productIds = products.stream().map(Product::getId).toList();
        Map<Long, List<ProductImage>> imagesByProduct = imageRepository
                .findByProductIdInOrderBySortOrderAscIdAsc(productIds).stream()
                .collect(Collectors.groupingBy(ProductImage::getProductId));

        Set<Long> brandIds = new LinkedHashSet<>();
        products.forEach(p -> brandIds.add(p.getBrand().getId()));
        List<Offer> offers = offerRepository.findActiveForBrands(brandIds, Instant.now());

        return products.stream().map(p -> 
        {
            List<Offer> brandOffers = offers.stream()
                    .filter(o -> o.getBrandId().equals(p.getBrand().getId()))
                    .toList();
            return ProductResponse.from(p, imagesByProduct.getOrDefault(p.getId(), List.of()), brandOffers);
        }).toList();
    }

    private String toJson(Object payload) 
    {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize event payload", e);
        }
    }
}
