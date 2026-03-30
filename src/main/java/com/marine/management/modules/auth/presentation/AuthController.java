// modules/auth/presentation/AuthController.java
package com.marine.management.modules.auth.presentation;

import com.marine.management.modules.auth.application.AuthService;
import com.marine.management.modules.auth.application.PasswordResetService;
import com.marine.management.modules.auth.application.RefreshTokenService;
import com.marine.management.modules.auth.application.RegistrationService;
import com.marine.management.modules.auth.domain.commands.AuthResult;
import com.marine.management.modules.auth.domain.commands.LoginCommand;
import com.marine.management.modules.auth.infrastructure.JwtUtil;
import com.marine.management.modules.auth.presentation.dto.AuthResponse;
import com.marine.management.modules.auth.presentation.dto.LoginRequest;
import com.marine.management.modules.auth.presentation.dto.RefreshTokenRequest;
import com.marine.management.modules.auth.presentation.dto.RefreshTokenResponse;
import com.marine.management.modules.auth.presentation.dto.RegisterRequest;
import com.marine.management.modules.auth.presentation.dto.RegisterResponse;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.security.PublicEndpoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    private final RegistrationService registrationService;
    private final PasswordResetService passwordResetService;

    public AuthController(
            AuthService authService,
            RefreshTokenService refreshTokenService,
            JwtUtil jwtUtil,
            RegistrationService registrationService,
            PasswordResetService passwordResetService
    ) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.jwtUtil = jwtUtil;
        this.registrationService = registrationService;
        this.passwordResetService = passwordResetService;
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

    /**
     * Register a new user with minimal information.
     * Creates organization + unverified user, sends verification email.
     *
     * PUBLIC: No authentication required.
     */
    @PostMapping("/register")
    @PublicEndpoint(reason = "User registration - creates new account")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        String email = registrationService.register(
                request.yachtName(),
                request.companyName(),
                request.firstName(),
                request.lastName(),
                request.email(),
                request.password(),
                request.phoneNumber()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RegisterResponse.success(email));
    }

    /**
     * Verify user's email address using verification token.
     *
     * PUBLIC: No authentication required.
     */
    @GetMapping("/verify-email")
    @PublicEndpoint(reason = "Email verification - activates user account")
    public ResponseEntity<Map<String, Object>> verifyEmail(
            @RequestParam("token") String token
    ) {
        registrationService.verifyEmail(token);

        return ResponseEntity.ok(Map.of(
                "message", "Email verified successfully. You can now log in.",
                "verified", true
        ));
    }

    /**
     * Request a password reset link.
     *
     * Generates a one-time token and sends it to the user's email.
     * Always responds with 200 OK to prevent user enumeration attacks.
     *
     * PUBLIC: No authentication required.
     */
    @PostMapping("/forgot-password")
    @PublicEndpoint(reason = "Password reset request - generates reset token")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @RequestBody Map<String, String> request
    ) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }

        passwordResetService.requestPasswordReset(email);

        // Always return the same message — prevents user enumeration attacks
        return ResponseEntity.ok(Map.of(
                "message", "If that email address is registered, you will receive a password reset link shortly."
        ));
    }

    /**
     * Reset password using a valid reset token.
     *
     * Validates the token, updates the password, and clears the token.
     * Token is single-use and expires in 1 hour.
     *
     * PUBLIC: No authentication required.
     */
    @PostMapping("/reset-password")
    @PublicEndpoint(reason = "Password reset - validates token and updates password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestBody Map<String, String> request
    ) {
        String token = request.get("token");
        String newPassword = request.get("password");

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Reset token is required"));
        }

        try {
            passwordResetService.resetPassword(token, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password reset successfully. You can now log in."));
        } catch (PasswordResetService.InvalidPasswordResetTokenException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Resend email verification link.
     *
     * PUBLIC: No authentication required.
     */
    @PostMapping("/resend-verification")
    @PublicEndpoint(reason = "Resend verification email")
    public ResponseEntity<Map<String, String>> resendVerification(
            @RequestBody Map<String, String> request
    ) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }

        registrationService.resendVerification(email);

        return ResponseEntity.ok(Map.of(
                "message", "Verification email sent. Please check your inbox."
        ));
    }
}