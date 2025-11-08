package com.marine.management.shared.exceptions;

public class UserRegistrationException extends RuntimeException{

    public UserRegistrationException(String message){
        super(message);
    }

    public UserRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }

    // Factory methods for common cases
    public static UserRegistrationException usernameAlreadyExists(String username) {
        return new UserRegistrationException("Username already exists: " + username);
    }

    public static UserRegistrationException emailAlreadyExists(String email) {
        return new UserRegistrationException("Email already exists: " + email);
    }
}
