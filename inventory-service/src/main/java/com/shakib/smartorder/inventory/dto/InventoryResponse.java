package com.shakib.smartorder.inventory.dto;


import java.time.Instant;

import com.shakib.smartorder.inventory.entity.Inventory;

public record InventoryResponse(
        Long productId,
        int availableQuantity,
        Instant updatedAt) {

    public static InventoryResponse from(Inventory i) 
    {
        return new InventoryResponse(i.getProductId(), i.getAvailableQuantity(), i.getUpdatedAt());
    }
}

