package com.shakib.smartorder.product.controller;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shakib.smartorder.product.dto.PageResponse;
import com.shakib.smartorder.product.dto.ProductRequest;
import com.shakib.smartorder.product.dto.ProductResponse;
import com.shakib.smartorder.product.enums.ProductCategory;
import com.shakib.smartorder.product.security.CurrentUser;
import com.shakib.smartorder.product.service.ProductFilter;
import com.shakib.smartorder.product.service.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    // Seller (own approved brand) or admin (platform brand).
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request,
            Authentication authentication)
    {
        ProductResponse created = service.create(CurrentUser.from(authentication), request);
        return ResponseEntity.created(URI.create("/api/v1/products/" + created.id())).body(created);
    }

    /*
      Public search. sort: newest (default) | price_asc | price_desc | name.
      clothingSize filters on the product's size label (S, M, 42, ...).
     */
    @GetMapping
    public PageResponse<ProductResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String clothingSize,
            @RequestParam(required = false) String color,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size)
    {
        ProductFilter filter = new ProductFilter(q, category, brandId, minPrice, maxPrice, clothingSize, color, sort);
        return service.search(filter, page, size);
    }

    // The calling seller's own products, including inactive ones.
    @GetMapping("/mine")
    public PageResponse<ProductResponse> mine(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, Authentication authentication)
    {
        return service.mine(CurrentUser.from(authentication), page, size);
    }

    // Up to 50 products by id, e.g. /batch?ids=1,2,3. Unknown or hidden products are left out.
    @GetMapping("/batch")
    public List<ProductResponse> batch(@RequestParam List<Long> ids) 
    {
        return service.batch(ids);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id)
    {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request,
            Authentication authentication) {
        return service.update(id, CurrentUser.from(authentication), request);
    }

    // Hides the product from the shop (soft delete). Existing orders keep their snapshot.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id, Authentication authentication)
    {
        service.deactivate(id, CurrentUser.from(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable Long id, Authentication authentication) 
    {
        service.activate(id, CurrentUser.from(authentication));
        return ResponseEntity.noContent().build();
    }
}
