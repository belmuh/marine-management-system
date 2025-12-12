package com.marine.management.modules.auth.presentation.dto;

import com.marine.management.modules.auth.presentation.UserResponse;
import com.marine.management.modules.users.domain.User;

public record AuthResponse(
        UserResponse user,
        String accessToken,
        String refreshToken,
        long accessTokenExpiry,
        long refreshTokenExpiry
) {
    // Factory method - User entity'den oluştur
    public static AuthResponse from(User user, String accessToken, String refreshToken,
                                    long accessTokenExpiry, long refreshTokenExpiry) {
        return new AuthResponse(
                UserResponse.from(user),
                accessToken,
                refreshToken,
                accessTokenExpiry,
                refreshTokenExpiry
        );
    }
}