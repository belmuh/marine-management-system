package com.marine.management.modules.auth.domain;

import java.time.LocalDateTime;

public class JwtToken {
    private final String token;
    private final LocalDateTime expiresAt;
    private final String tokenType;

    public JwtToken(String token, LocalDateTime expiresAt, String tokenType) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.tokenType = tokenType;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public String getToken() { return token; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public String getTokenType() { return tokenType; }
}
