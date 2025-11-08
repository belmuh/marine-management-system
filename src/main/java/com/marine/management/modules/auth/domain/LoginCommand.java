package com.marine.management.modules.auth.domain;

public record LoginCommand(String username, String password) {
    public LoginCommand {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()){
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
    }
}
