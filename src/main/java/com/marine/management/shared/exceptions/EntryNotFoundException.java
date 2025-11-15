package com.marine.management.shared.exceptions;

import java.util.UUID;

public class EntryNotFoundException extends RuntimeException{

    public EntryNotFoundException(String message) {
        super(message);
    }

    // Factory methods - daha clean usage
    public static EntryNotFoundException withId(String id) {
        return new EntryNotFoundException("Entry not found with id: " + id);
    }

    public static EntryNotFoundException withId(UUID id) {
        return new EntryNotFoundException("Entry not found with id: " + id);
    }
}
