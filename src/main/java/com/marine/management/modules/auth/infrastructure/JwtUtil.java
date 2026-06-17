package com.marine.management.modules.auth.infrastructure;

import com.marine.management.shared.security.TenantAwareUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT utility for multi-tenant authentication.
 *
 * CLAIMS STRATEGY:
 * - Minimal claims (identity + tenant context only)
 * - Authorities loaded from SecurityContext, not JWT
 * - Role changes don't require token invalidation
 */
@Component
public class JwtUtil {

    //  Constants for claim names
    public static final String CLAIM_TENANT_ID = "tenantId";
    public static final String CLAIM_ROLE = "role";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationMs;

    @Value("${refresh.token.expiration}")
    private long refreshExpirationMs;

    @PostConstruct
    public void validateSecret() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                "JWT secret must be at least 256 bits (32 characters). " +
                "Set a strong value for the JWT_SECRET environment variable."
            );
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate JWT with minimal claims.
     *
     * CLAIMS:
     * - sub: username (identity)
     * - tenantId: organization ID (tenant context)
     * - role: user role (optional, for convenience)
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        //  Polymorphic tenant-aware support
        if (userDetails instanceof TenantAwareUserDetails tenantUser) {
            claims.put(CLAIM_TENANT_ID, tenantUser.getTenantId());
            claims.put(CLAIM_ROLE, tenantUser.getRole());
        }

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract tenant ID from JWT.
     * Used by TenantFilter for tenant context establishment.
     */
    public Long extractTenantId(String token) {
        Claims claims = extractAllClaims(token);
        Object tenantId = claims.get(CLAIM_TENANT_ID);  //  Use constant

        if (tenantId == null) {
            return null;
        }

        // Handle Integer/Long conversion
        if (tenantId instanceof Integer) {
            return ((Integer) tenantId).longValue();
        }

        return (Long) tenantId;
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = extractAllClaims(token);
            String username = claims.getSubject();
            Date expiration = claims.getExpiration();

            return username.equals(userDetails.getUsername())
                    && expiration.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }
}