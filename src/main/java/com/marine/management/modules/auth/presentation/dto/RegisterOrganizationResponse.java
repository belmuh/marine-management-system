package com.marine.management.modules.auth.presentation.dto;

import java.util.UUID;

public record RegisterOrganizationResponse(
        Long organizationId,
        String organizationName,
        UUID adminUserId,
        String adminEmail,
        String message
) {}