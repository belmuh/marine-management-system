package com.marine.management.modules.auth.presentation;

import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.kernel.security.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        Role role,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
          user.getId(),
          user.getUsername(),
          user.getEmail(),
          user.getRole(),
          user.getCreatedAt()
        );
    }
}
