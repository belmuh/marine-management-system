package com.marine.management.modules.organization.application.commands;

import java.util.UUID;

/**
 * Result of successful yacht registration.
 */
public record OnboardingResult(
        Long organizationId,
        UUID userId,
        String yachtName,
        String email
) {}