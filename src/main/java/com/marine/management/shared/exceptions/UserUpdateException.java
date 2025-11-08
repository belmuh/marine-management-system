package com.marine.management.shared.exceptions;

public class UserUpdateException extends RuntimeException{

    public UserUpdateException(String message){
        super(message);
    }

    // Factory methods
    public static UserUpdateException usernameTaken(String username){
        return new UserUpdateException("Username already taken: " + username);
    }

    public static UserUpdateException emailRegistered(String email) {
        return new UserUpdateException("Email already registered: " + email);
    }

    public static UserUpdateException invalidOperation(String operation) {
        return new UserUpdateException("Invalid operation: " + operation);
    }
}
