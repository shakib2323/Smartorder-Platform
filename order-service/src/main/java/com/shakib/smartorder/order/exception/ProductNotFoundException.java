package com.shakib.smartorder.order.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long productId) {
        super("Product " + productId + " does not exist");
    }
}
