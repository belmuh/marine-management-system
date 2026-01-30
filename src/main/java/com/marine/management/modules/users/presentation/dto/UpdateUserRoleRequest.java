package com.marine.management.modules.users.presentation.dto;

import com.marine.management.shared.security.Role;
import jakarta.validation.constraints.NotNull;

/**
 * Request for updating user role.
 */
public record UpdateUserRoleRequest(
        @NotNull(message = "Role is required")
        Role role
) {}