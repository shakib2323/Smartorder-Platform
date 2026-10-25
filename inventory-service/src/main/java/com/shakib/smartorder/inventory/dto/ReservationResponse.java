package com.shakib.smartorder.inventory.dto;

import java.util.List;

import com.shakib.smartorder.inventory.entity.StockReservation;
import com.shakib.smartorder.inventory.enums.ReservationStatus;

public record ReservationResponse(Long orderId, List<Item> items) {

    public record Item(Long productId, int quantity, ReservationStatus status)
    {
    }

    public static ReservationResponse from(Long orderId, List<StockReservation> reservations) 
    {
        List<Item> items = reservations.stream()
                .map(r -> new Item(r.getProductId(), r.getQuantity(), r.getStatus()))
                .toList();
        return new ReservationResponse(orderId, items);
    }
}
