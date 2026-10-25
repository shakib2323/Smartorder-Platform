package com.shakib.smartorder.auth.exception;

public class TooManyAttemptsException extends RuntimeException {

    public TooManyAttemptsException() {
        super("Too many failed login attempts. Please try again later.");
    }
}
