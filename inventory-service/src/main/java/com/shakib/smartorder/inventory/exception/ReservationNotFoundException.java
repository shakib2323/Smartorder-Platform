package com.shakib.smartorder.inventory.exception;


public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(Long orderId) {
        super("No reservation found for order " + orderId);
    }
}

