package com.marine.management.modules.auth.presentation;

import com.marine.management.modules.auth.application.AuthService;
import com.marine.management.modules.auth.domain.AuthResult;
import com.marine.management.modules.auth.domain.LoginCommand;
import com.marine.management.modules.users.domain.User;
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

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/login")
   // @RateLimit(limit = 5, duration = 15) // 15 dakikada 5 deneme
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {


        LoginCommand command = new LoginCommand(request.username(), request.password());
        AuthResult authResult = authService.login(command);

        AuthResponse response = new AuthResponse(authResult.getToken(), authResult.getUser());

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


    public record LoginRequest(
            @NotBlank(message = "Username cannot be blank")
            @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
            @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers and underscores")
            String username,

            @NotBlank(message = "Password cannot be blank")
            @Size(min = 8, message = "Password must be at least 8 characters")
            String password
    ) {}
    public record AuthResponse(String token, UserResponse user) {}
}
