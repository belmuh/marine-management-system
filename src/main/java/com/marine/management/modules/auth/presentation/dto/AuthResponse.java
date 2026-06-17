package com.marine.management.modules.auth.presentation.dto;

import com.marine.management.modules.auth.presentation.UserResponse;
import com.marine.management.modules.users.domain.User;

import java.util.Set;

public record AuthResponse(
        UserResponse user,
        String accessToken,
        long accessTokenExpiry,
        long refreshTokenExpiry,
        Set<String> permissions,
        boolean onboardingCompleted
) {
    public static AuthResponse from(User user, String accessToken,
                                    long accessTokenExpiry, long refreshTokenExpiry) {
        boolean onboardingDone = user.getOrganization() != null
                && user.getOrganization().isOnboardingCompleted();

        return new AuthResponse(
                UserResponse.from(user),
                accessToken,
                accessTokenExpiry,
                refreshTokenExpiry,
                user.getRoleEnum().getPermissionNames(),
                onboardingDone
        );
    }
}