package com.marine.management.modules.organization.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
        // Organization
        @NotBlank(message = "Yacht name is required")
        @Size(min = 2, max = 100)
        String yachtName,

        @Size(max = 100)
        String companyName,

        @NotBlank(message = "Flag country is required")
        @Size(min = 2, max = 2)
        String flagCountry,

        @NotBlank(message = "Base currency is required")
        @Size(min = 3, max = 3)
        String baseCurrency,

        String yachtType,
        Integer yachtLength,
        String homeMarina,

        // User
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50)
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        String firstName,
        String lastName
) {}

