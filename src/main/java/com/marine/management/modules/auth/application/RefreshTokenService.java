package com.marine.management.modules.auth.application;

import com.marine.management.modules.auth.domain.RefreshToken;
import com.marine.management.modules.auth.infrastructure.RefreshTokenRepository;
import com.marine.management.modules.users.domain.User;
import com.marine.management.modules.users.infrastructure.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Transactional
public class RefreshTokenService {

    @Value("${refresh.token.expiration:604800000}")
    private long refreshTokenExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a refresh token for the user.
     * The raw token is returned to the caller (for sending to the client);
     * only its SHA-256 hash is persisted in the database.
     */
    public String createRefreshToken(UUID userId, String ipAddress, String userAgent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        refreshTokenRepository.deleteByUserId(userId);

        String rawToken = generateSecureToken();

        LocalDateTime expiry = LocalDateTime.now()
                .plusSeconds(refreshTokenExpirationMs / 1000);

        RefreshToken refreshToken = new RefreshToken(
                hashToken(rawToken),
                user,
                expiry,
                ipAddress,
                userAgent
        );

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    /**
     * Validates the raw refresh token sent by the client.
     * Hashes it before looking up in the database.
     */
    public User validateRefreshToken(String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(hashToken(rawToken))
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token expired");
        }

        return refreshToken.getUser();
    }

    /**
     * Deletes the refresh token matching the raw token sent by the client.
     */
    public void deleteRefreshToken(String rawToken) {
        refreshTokenRepository.findByToken(hashToken(rawToken))
                .ifPresent(refreshTokenRepository::delete);
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[64];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}