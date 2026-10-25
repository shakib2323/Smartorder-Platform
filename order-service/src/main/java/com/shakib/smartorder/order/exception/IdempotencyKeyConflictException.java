package com.shakib.smartorder.order.exception;

public class IdempotencyKeyConflictException extends RuntimeException {

    public IdempotencyKeyConflictException() {
        super("This Idempotency-Key was already used with a different request body");
    }
}
