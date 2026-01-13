package com.marine.management.modules.auth.presentation;

import com.marine.management.modules.users.domain.User;

public record UserResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        String role,
        String organizationName,
        Long organizationId
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getOrganization().getYachtName(),
                user.getOrganization().getId()
        );
    }
}
