package com.marine.management.modules.auth.presentation.dto;

public record RefreshTokenResponse(
         String accessToken,
         Long expiresIn
) {
}
