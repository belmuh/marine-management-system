package com.marine.management.modules.auth.presentation;

import com.marine.management.modules.auth.application.AuthService;
import com.marine.management.modules.auth.application.RefreshTokenService;
import com.marine.management.modules.auth.domain.AuthResult;
import com.marine.management.modules.auth.domain.LoginCommand;
import com.marine.management.modules.auth.infrastructure.JwtUtil;
import com.marine.management.modules.auth.presentation.dto.AuthResponse;
import com.marine.management.modules.auth.presentation.dto.LoginRequest;
import com.marine.management.modules.auth.presentation.dto.RefreshTokenRequest;
import com.marine.management.modules.auth.presentation.dto.RefreshTokenResponse;
import com.marine.management.modules.users.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService,
                          RefreshTokenService refreshTokenService,
                          JwtUtil jwtUtil){
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
   // @RateLimit(limit = 5, duration = 15) // 15 dakikada 5 deneme
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {

        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        LoginCommand command = new LoginCommand(request.username(), request.password());
        AuthResult authResult = authService.login(command, ipAddress, userAgent);

        AuthResponse response = AuthResponse.from(
                authResult.user(),
                authResult.accessToken(),
                authResult.refreshToken(),
                authResult.accessTokenExpiry(),
                authResult.refreshTokenExpiry());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal User user){
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserResponse userResponse = UserResponse.from(user);
        return ResponseEntity.ok(userResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(
            @RequestBody RefreshTokenRequest request) {

        try {
            // Refresh token'ı doğrula
            User user = refreshTokenService.validateRefreshToken(request.refreshToken());

            // Yeni access token oluştur
            String newAccessToken = jwtUtil.generateToken(user);

            RefreshTokenResponse response = new RefreshTokenResponse(newAccessToken, jwtUtil.getExpirationMs());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest request) {
        refreshTokenService.deleteRefreshToken(request.refreshToken());
        return ResponseEntity.ok().build();
    }



}
