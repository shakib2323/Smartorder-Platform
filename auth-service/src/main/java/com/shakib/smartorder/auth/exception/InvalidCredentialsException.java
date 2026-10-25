package com.shakib.smartorder.auth.exception;

// One message for no such email and wrong password, so the API never reveals which emails exist. 
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() 
    {
        super("Invalid email or password");
    }
}
