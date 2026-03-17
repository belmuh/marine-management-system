// modules/auth/presentation/AuthController.java
package com.marine.management.modules.auth.presentation;

import com.marine.management.modules.auth.application.AuthService;
import com.marine.management.modules.auth.application.RefreshTokenService;
import com.marine.management.modules.auth.domain.commands.AuthResult;
import com.marine.management.modules.auth.domain.commands.LoginCommand;
import com.marine.management.modules.auth.infrastructure.JwtUtil;
import com.marine.management.modules.auth.presentation.dto.AuthResponse;
import com.marine.management.modules.auth.presentation.dto.LoginRequest;
import com.marine.management.modules.auth.presentation.dto.RefreshTokenRequest;
import com.marine.management.modules.auth.presentation.dto.RefreshTokenResponse;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.security.PublicEndpoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller for user login, token refresh, and profile access.
 *
 * PUBLIC ENDPOINTS:
 * - POST /api/auth/login: Authenticate user with email and password
 * - POST /api/auth/refresh: Refresh access token using refresh token
 *
 * PROTECTED ENDPOINTS:
 * - GET /api/auth/me: Get current authenticated user profile
 * - POST /api/auth/logout: Invalidate refresh token
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;

    public AuthController(
            AuthService authService,
            RefreshTokenService refreshTokenService,
            JwtUtil jwtUtil
    ) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * User login endpoint.
     *
     * Authenticates user with email (sent as "username" field) and password.
     * Returns JWT access token and refresh token.
     *
     * PUBLIC: No authentication required.
     *
     * @param request Login request containing username (email) and password
     * @param httpRequest HTTP request for IP address and user agent extraction
     * @return AuthResponse with user info, access token, and refresh token
     */
    @PostMapping("/login")
    @PublicEndpoint(reason = "User authentication - creates JWT token")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        // ⭐ LoginCommand: username field contains email
        LoginCommand command = new LoginCommand(request.email(), request.password());

        AuthResult authResult = authService.login(command, ipAddress, userAgent);

        AuthResponse response = AuthResponse.from(
                authResult.user(),
                authResult.accessToken(),
                authResult.refreshToken(),
                authResult.accessTokenExpiry(),
                authResult.refreshTokenExpiry()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get current authenticated user profile.
     *
     * Returns user information extracted from JWT token.
     *
     * PROTECTED: Requires valid JWT token in Authorization header.
     *
     * @param user Current authenticated user (injected from SecurityContext)
     * @return UserResponse with user profile information
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserResponse userResponse = UserResponse.from(user);
        return ResponseEntity.ok(userResponse);
    }

    /**
     * Refresh access token using refresh token.
     *
     * Validates refresh token and generates new access token.
     * Does NOT generate new refresh token (use existing one).
     *
     * PUBLIC: Refresh token validation, no tenant context needed.
     *
     * @param request Refresh token request
     * @return RefreshTokenResponse with new access token
     */
    @PostMapping("/refresh")
    @PublicEndpoint(reason = "Token refresh - validates refresh token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(
            @RequestBody RefreshTokenRequest request
    ) {
        try {
            User user = refreshTokenService.validateRefreshToken(request.refreshToken());
            String newAccessToken = jwtUtil.generateToken(user);

            // Permission'ları da gönder — role değişmiş olabilir
            RefreshTokenResponse response = new RefreshTokenResponse(
                    newAccessToken,
                    jwtUtil.getExpirationMs(),
                    user.getRoleEnum().getPermissionNames()
            );

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    /**
     * Logout user by invalidating refresh token.
     *
     * Deletes refresh token from database to prevent further use.
     * Access token remains valid until expiration (stateless JWT).
     *
     * PROTECTED: Requires authentication.
     *
     * @param request Refresh token to invalidate
     * @return Empty response with 200 OK status
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest request) {
        refreshTokenService.deleteRefreshToken(request.refreshToken());
        return ResponseEntity.ok().build();
    }
}