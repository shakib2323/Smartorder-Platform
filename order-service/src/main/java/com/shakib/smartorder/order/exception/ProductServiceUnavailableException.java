package com.shakib.smartorder.order.exception;

public class ProductServiceUnavailableException extends RuntimeException {

    public ProductServiceUnavailableException(Throwable cause) {
        super("Product service is temporarily unavailable, please retry shortly", cause);
    }
}
