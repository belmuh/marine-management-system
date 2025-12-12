package com.marine.management.modules.auth.presentation.dto;

import com.marine.management.modules.users.domain.User;

public record LoginResponse(
        User user,
        String accessToken,
        String refreshToken,
        Long accessTokenExpiry,
        Long refreshTokenExpiry
) {}