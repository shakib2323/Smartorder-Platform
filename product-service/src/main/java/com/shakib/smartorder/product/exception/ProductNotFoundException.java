package com.shakib.smartorder.product.exception;

import org.springframework.http.HttpStatus;

public class ProductNotFoundException extends ApiException {

    public ProductNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Product not found with id " + id);
    }
}
