package com.marine.management.modules.users.presentation;

import com.marine.management.modules.auth.presentation.UserResponse;
import com.marine.management.modules.finance.domain.FinancialCategory;
import com.marine.management.modules.finance.presentation.dto.CategoryResponseDto;
import com.marine.management.modules.users.application.UserService;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.kernel.security.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<UserResponse>> getAllUsers(){
        List<User> users = userService.getAllUsers();
        List<UserResponse> response = users.stream()
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        User user = userService.getUserByIdOrThrow(userId);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.registerUserWithRole(
                request.username(),
                request.email(),
                request.password(),
                request.role()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PutMapping("/{userId}/profile")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or #userId == authentication.principal.id")
    public ResponseEntity<UserResponse> updateUserProfile(@PathVariable UUID userId,
                                                          @Valid @RequestBody UpdateProfileRequest request) {
        User user = userService.updateUserProfile(userId, request.username(), request.email());
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserRole(@PathVariable UUID userId,
                                                       @Valid @RequestBody UpdateRoleRequest request) {
        User user = userService.updateUserRole(userId, request.role());
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PutMapping("/{userId}/password")
    @PreAuthorize("#userId == authentication.principal.id")
    public ResponseEntity<Void> changePassword(
            @PathVariable UUID userId,
            @Valid @RequestBody ChangePasswordRequest request) {

        userService.changeUserPassword(userId, request.newPassword());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> activate(@PathVariable UUID id) {
        User user = userService.activate(id);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> deactivate(@PathVariable UUID id) {
        User user = userService.deactivate(id);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId){
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check-username")
    public ResponseEntity<AvailabilityResponse> checkUsernameAvailability(
            @RequestParam String username) {
        boolean available = userService.isUsernameAvailable(username);
        return ResponseEntity.ok(new AvailabilityResponse(available));
    }

    @GetMapping("/check-email")
    public ResponseEntity<AvailabilityResponse> checkEmailAvailability(@RequestParam String email) {
        boolean available = userService.isEmailAvailable(email);
        return ResponseEntity.ok(new AvailabilityResponse(available));
    }

    // === REQUEST DTOs ===
    public record CreateUserRequest(
            @NotBlank(message = "Username cannot be blank")
            @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
            @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers and underscores")
            String username,

            @NotBlank(message = "Email cannot be blank")
            @Email(message = "Email should be valid")
            String email,

            @NotBlank(message = "Password cannot be blank")
            @Size(min=8, message = "Password must be at least 8 characters")
            String password,

            Role role
    ){}

    public record UpdateProfileRequest(
            @NotBlank(message = "Username cannot be blank")
            @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
            @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers and underscores")
            String username,

            @NotBlank(message = "Email cannot be blank")
            @Email(message = "Email should be valid")
            String email
    ) {}

    public record UpdateRoleRequest(Role role) {}

    public record ChangePasswordRequest(
            @NotBlank(message = "New password cannot be blank")
            @Size(min = 8, message = "New password must be at least 8 characters")
            String newPassword
    ) {}

    public record AvailabilityResponse(boolean available) {}



}
