package com.shakib.smartorder.auth.exception;

public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException() {
        super("That email is already registered");
    }
}
