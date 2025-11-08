package com.marine.management.shared.exceptions;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException{

    public UserNotFoundException(String message){
        super(message);
    }

    // Factory methods
    public static UserNotFoundException withId(UUID userId){
        return new UserNotFoundException("User not found with id: " + userId);
    }

    public static UserNotFoundException withUsername(String username){
        return new UserNotFoundException("User not found with username: " + username);
    }

    public static UserNotFoundException withEmail(String email){
        return new UserNotFoundException("User not found with email: " + email);
    }
}
