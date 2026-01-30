// modules/users/presentation/dto/UserDTO.java
package com.marine.management.modules.users.presentation.dto;

import com.marine.management.modules.users.domain.User;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for User Management.
 *
 * Used for:
 * - GET /api/users (list all users)
 * - GET /api/users/{id} (get user details)
 * - POST /api/users (create user response)
 * - PUT /api/users/{id}/... (update operations response)
 *
 * Contains full user information for management purposes.
 * Separate from Auth module's UserResponse which is for session context.
 */
public record UserDTO(
        String id,
        String email,
        String firstName,
        String lastName,
        String role,
        boolean active,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long tenantId
) {
    public static UserDTO from(User user) {
        return new UserDTO(
                user.getId().toString(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),  // Enum to String
                user.isActive(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getTenantId()
        );
    }
}