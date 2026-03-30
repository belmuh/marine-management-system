package com.marine.management.modules.organization.presentation.dto;

/**
 * Setup completion response DTO.
 */
public record SetupResponse(
    Long organizationId,
    String yachtName,
    String message,
    boolean onboardingCompleted
) {
    public static SetupResponse success(Long orgId, String yachtName) {
        return new SetupResponse(
            orgId,
            yachtName,
            "Setup completed successfully.",
            true
        );
    }
}
