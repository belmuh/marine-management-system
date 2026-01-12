package com.marine.management.modules.organization.application.commands;

/**
 * Command to register a new yacht organization with owner user.
 *
 * Creates:
 * - Organization (tenant)
 * - Admin user (yacht owner)
 * - Default tenant setup (WHO selections, main categories)
 */
public record RegisterYachtCommand(
        // Organization fields
        String yachtName,
        String companyName,
        String flagCountry,
        String baseCurrency,
        String yachtType,
        Integer yachtLength,
        String homeMarina,

        // User fields
        String username,
        String email,
        String password,
        String firstName,
        String lastName
) {
    /**
     * Compact constructor with validation.
     */
    public RegisterYachtCommand {
        // Organization validation
        if (yachtName == null || yachtName.trim().isEmpty()) {
            throw new IllegalArgumentException("Yacht name is required");
        }
        if (yachtName.length() > 100) {
            throw new IllegalArgumentException("Yacht name cannot exceed 100 characters");
        }

        if (flagCountry == null || flagCountry.length() != 2) {
            throw new IllegalArgumentException("Valid flag country (2-letter ISO code) is required");
        }

        if (baseCurrency == null || baseCurrency.length() != 3) {
            throw new IllegalArgumentException("Valid currency (3-letter ISO code) is required");
        }

        // User validation
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (username.length() < 3 || username.length() > 50) {
            throw new IllegalArgumentException("Username must be between 3 and 50 characters");
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Username can only contain letters, numbers and underscores");
        }

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (password.length() > 100) {
            throw new IllegalArgumentException("Password cannot exceed 100 characters");
        }
    }
}