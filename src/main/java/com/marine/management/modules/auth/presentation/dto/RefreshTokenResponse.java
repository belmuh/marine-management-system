package com.marine.management.modules.auth.presentation.dto;

import java.util.Set;

public record RefreshTokenResponse(
         String accessToken,
         Long expiresIn,
         Set<String> permissions
) {
}
