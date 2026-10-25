package com.shakib.smartorder.order.exception;

public class ProductInactiveException extends RuntimeException {

    public ProductInactiveException(Long productId) {
        super("Product " + productId + " is not available for sale");
    }
}
