package com.shakib.smartorder.product.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.shakib.smartorder.product.entity.Brand;
import com.shakib.smartorder.product.entity.Product;
import com.shakib.smartorder.product.enums.BrandStatus;
import com.shakib.smartorder.product.enums.ProductCategory;

import jakarta.persistence.criteria.Join;

// Building blocks for the product search. Each one adds a single filter.
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    // What shoppers may see: active product of an approved brand. 
    public static Specification<Product> publiclyVisible() {
        return (root, query, cb) -> {
            Join<Product, Brand> brand = root.join("brand");
            return cb.and(cb.isTrue(root.get("active")), cb.equal(brand.get("status"), BrandStatus.APPROVED));
        };
    }

    public static Specification<Product> ownedBy(Long ownerUserId) 
    {
        return (root, query, cb) -> {
            Join<Product, Brand> brand = root.join("brand");
            return cb.equal(brand.get("ownerUserId"), ownerUserId);
        };
    }

    public static Specification<Product> idIn(Collection<Long> ids) 
    {
        return (root, query, cb) -> root.get("id").in(ids);
    }

    public static Specification<Product> nameContains(String text)
    {
        String pattern = "%" + escapeLike(text.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, '\\');
    }

    public static Specification<Product> inCategory(ProductCategory category)
    {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<Product> ofBrand(Long brandId)
    {
        return (root, query, cb) -> cb.equal(root.get("brand").get("id"), brandId);
    }

    public static Specification<Product> priceAtLeast(BigDecimal min)
    {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<BigDecimal>get("price"), min);
    }

    public static Specification<Product> priceAtMost(BigDecimal max) 
    {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.<BigDecimal>get("price"), max);
    }

    public static Specification<Product> hasSize(String size) 
    {
        String wanted = size.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(cb.lower(root.get("size")), wanted);
    }

    public static Specification<Product> hasColor(String color)
    {
        String wanted = color.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(cb.lower(root.get("color")), wanted);
    }

    // User text must not act as a LIKE wildcard ("%" would match everything).
    static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
