package com.marine.management.modules.auth.domain.commands;

import com.marine.management.modules.users.domain.User;

import java.util.Set;

public record AuthResult(
        String accessToken,
        String refreshToken,
        User user,
        long accessTokenExpiry,
        long refreshTokenExpiry,
        Set<String> permissions
) {
    public static AuthResult from(String accessToken, String refreshToken, User user,
                                  long accessTokenExpiry, long refreshTokenExpiry) {
        return new AuthResult(
                accessToken,
                refreshToken,
                user,
                accessTokenExpiry,
                refreshTokenExpiry,
                user.getRoleEnum().getPermissionNames()
        );
    }
}