package com.shakib.smartorder.product.service;

import java.math.BigDecimal;

import com.shakib.smartorder.product.enums.ProductCategory;

public record ProductFilter(String text, ProductCategory category, Long brandId, BigDecimal minPrice,
        BigDecimal maxPrice, String size, String color, String sort) {
}
