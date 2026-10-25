package com.shakib.smartorder.inventory.exception;

public class StockAccessDeniedException extends RuntimeException {

    public StockAccessDeniedException(Long productId) {
        super("You can only change stock of your own products (product " + productId + ")");
    }
}
