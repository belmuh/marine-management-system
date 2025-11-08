package com.marine.management.shared.exceptions;

public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException(String message){
        super(message);
    }
}
