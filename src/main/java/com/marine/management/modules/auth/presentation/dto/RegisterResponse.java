package com.marine.management.modules.auth.presentation.dto;

/**
 * Registration response DTO.
 * Contains minimal info - user needs to verify email before they can do anything.
 */
public record RegisterResponse(
    String message,
    String email,
    boolean requiresEmailVerification
) {
    public static RegisterResponse success(String email) {
        return new RegisterResponse(
            "Registration successful. Please check your email to verify your account.",
            email,
            true
        );
    }
}
