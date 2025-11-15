package com.marine.management.shared.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CategoryNotFoundException extends RuntimeException{
    public CategoryNotFoundException(String message) {
        super(message);
    }

    // Factory methods - daha clean usage
    public static CategoryNotFoundException withId(String id) {
        return new CategoryNotFoundException("Category not found with id: " + id);
    }

    public static CategoryNotFoundException withId(UUID id) {
        return new CategoryNotFoundException("Category not found with id: " + id);
    }

    public static CategoryNotFoundException withCode(String code) {
        return new CategoryNotFoundException("Category not found with code: " + code);
    }

    public static CategoryNotFoundException withName(String name) {
        return new CategoryNotFoundException("Category not found with name: " + name);
    }
}
