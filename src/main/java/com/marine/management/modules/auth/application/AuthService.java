// modules/auth/application/AuthService.java
package com.marine.management.modules.auth.application;

import com.marine.management.modules.auth.domain.commands.AuthResult;
import com.marine.management.modules.auth.domain.Authentication;
import com.marine.management.modules.auth.domain.commands.LoginCommand;
import com.marine.management.modules.auth.infrastructure.JwtUtil;
import com.marine.management.modules.auth.presentation.UserResponse;
import com.marine.management.modules.users.domain.User;
import com.marine.management.modules.users.infrastructure.UserRepository;
import com.marine.management.shared.exceptions.AuthenticationFailedException;
import com.marine.management.shared.exceptions.UnauthorizedAccessException;
import com.marine.management.shared.exceptions.UserNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository,
                       JwtUtil jwtUtil,
                       PasswordEncoder passwordEncoder,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    public boolean authenticate(User user, String inputPassword) {
        if (user == null) {
            return false;
        }
        return user.credentialsMatch(inputPassword, passwordEncoder)
                && Authentication.canGenerateTokenForUser(user);
    }

    /**
     * Login user with email and password.
     *
     * @param command LoginCommand containing username (email) and password
     * @param ipAddress Client IP address
     * @param userAgent Client user agent
     * @return AuthResult with tokens and user info
     * @throws AuthenticationFailedException if credentials invalid or user inactive
     * @throws UnauthorizedAccessException if user cannot generate token
     */
    @Transactional
    public AuthResult login(LoginCommand command, String ipAddress, String userAgent) {
        // ⭐ CLEAN: command.username() contains email
        User user = findUserByEmail(command.username());

        if (!user.isActive()) {
            throw new AuthenticationFailedException("User account is disabled");
        }

        boolean authenticated = authenticate(user, command.password());

        if (!authenticated) {
            throw new AuthenticationFailedException("Invalid credentials");
        }

        if (!Authentication.canGenerateTokenForUser(user)) {
            throw new UnauthorizedAccessException("User cannot generate token");
        }

        user.updateLastLogin(LocalDateTime.now());

        // Generate tokens
        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(
                user.getUserId(),
                ipAddress,
                userAgent
        ).getToken();

        return new AuthResult(
                accessToken,
                refreshToken,
                user,
                jwtUtil.getExpirationMs(),
                jwtUtil.getRefreshExpirationMs()
        );
    }

    /**
     * Validate JWT token.
     *
     * @param token JWT access token
     * @return true if token is valid
     */
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    /**
     * Extract email from JWT token.
     *
     * @param token JWT access token
     * @return User email (Spring Security username)
     */
    public String extractEmailFromToken(String token) {
        return jwtUtil.extractUsername(token);  // Returns email (from UserDetails.getUsername())
    }

    /**
     * Get user entity from JWT token.
     *
     * @param token JWT access token
     * @return User entity
     * @throws UserNotFoundException if user not found
     */
    @Transactional
    public User getUserFromToken(String token) {
        String email = jwtUtil.extractUsername(token);
        return findUserByEmail(email);
    }

    // ============================================
    // PRIVATE HELPERS
    // ============================================

    /**
     * Find user by email (single source of truth).
     *
     * @param email User email address
     * @return User entity
     * @throws UserNotFoundException if user not found
     */
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }
}