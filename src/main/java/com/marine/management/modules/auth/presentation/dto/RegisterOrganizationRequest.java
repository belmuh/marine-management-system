package com.marine.management.modules.auth.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterOrganizationRequest(
        @NotBlank(message = "Organization name is required")
        @Size(min = 2, max = 100, message = "Organization name must be between 2 and 100 characters")
        String organizationName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String adminEmail,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
                message = "Password must contain at least one uppercase letter, one lowercase letter, and one number"
        )
        String password,

        @NotBlank(message = "Admin first name is required")
        @Size(min = 2, max = 50)
        String adminFirstName,

        @NotBlank(message = "Admin last name is required")
        @Size(min = 2, max = 50)
        String adminLastName,

        String phoneNumber,

        String country,

        String city
) {}