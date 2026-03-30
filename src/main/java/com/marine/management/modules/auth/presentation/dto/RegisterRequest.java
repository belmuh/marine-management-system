package com.marine.management.modules.auth.presentation.dto;

import jakarta.validation.constraints.*;

/**
 * Registration request DTO.
 * Simple registration: yacht name + user info only.
 * Financial settings, categories etc. are configured in the setup step after login.
 */
public record RegisterRequest(
    @NotBlank(message = "Yacht name is required")
    @Size(min = 2, max = 100, message = "Yacht name must be between 2 and 100 characters")
    String yachtName,

    String companyName,

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    String firstName,

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    String lastName,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    String password,

    String phoneNumber
) {}
