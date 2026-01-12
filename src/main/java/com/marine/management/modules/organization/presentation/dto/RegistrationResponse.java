package com.marine.management.modules.organization.presentation.dto;

public record RegistrationResponse(
        boolean success,
        String message,
        String yachtName,
        String email
) {}