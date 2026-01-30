// modules/auth/domain/commands/LoginCommand.java
package com.marine.management.modules.auth.domain.commands;

/**
 * Login command for user authentication.
 *
 * NOTE: Field is named "username" for consistency with Spring Security
 * and login identifier concept, but it contains user's email address.
 *
 * @param username User's email address (login identifier)
 * @param password User's password (plain text - will be validated against hashed password)
 */
public record LoginCommand(String username, String password) {

    public LoginCommand {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Email/username cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
    }
}