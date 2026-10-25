package com.shakib.smartorder.inventory.exception;


public class InventoryNotFoundException extends RuntimeException {

    public InventoryNotFoundException(Long productId) {
        super("No inventory record for product " + productId);
    }
}

