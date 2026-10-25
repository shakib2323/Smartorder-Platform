package com.shakib.smartorder.order.exception;

import com.shakib.smartorder.order.enums.OrderStatus;

public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(Long orderId, OrderStatus current, String action) {
        super("Order " + orderId + " is " + current + " and cannot be " + action);
    }
}
