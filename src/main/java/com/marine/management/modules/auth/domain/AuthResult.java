package com.marine.management.modules.auth.domain;

import com.marine.management.modules.users.domain.User;

public record AuthResult(
        String accessToken,
        String refreshToken,
        User user,  // ← UserResponse değil, User entity
        long accessTokenExpiry,
        long refreshTokenExpiry
) {}