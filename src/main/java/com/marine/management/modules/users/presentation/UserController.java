// modules/users/presentation/UserController.java
package com.marine.management.modules.users.presentation;

import com.marine.management.modules.users.application.UserService;
import com.marine.management.modules.users.domain.User;
import com.marine.management.modules.users.presentation.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * User management controller.
 *
 * All operations are tenant-scoped (users can only manage users within their organization).
 *
 * ADMIN operations:
 * - Create user
 * - Update user role
 * - Activate/deactivate user
 * - Delete user
 *
 * ADMIN/MANAGER operations:
 * - List all users
 * - Get user by ID
 *
 * USER operations:
 * - Update own profile
 * - Change own password
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * List all users in current organization.
     *
     * @return List of users in current tenant
     */
    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserDTO> response = users.stream()
                .map(UserDTO::from)  // UserDTO instead of UserResponse
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Get user by ID within current organization.
     *
     * @param userId User UUID
     * @return User details
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER_VIEW') or #userId == authentication.principal.id")
    public ResponseEntity<UserDTO> getUserById(@PathVariable UUID userId) {
        User user = userService.getUserByIdOrThrow(userId);
        return ResponseEntity.ok(UserDTO.from(user));
    }

    /**
     * Create new user in current organization.
     *
     * Only ADMIN can create users.
     * User will be created in the same organization as the current admin.
     *
     * @param request User creation request
     * @param currentUser Current authenticated admin user
     * @return Created user details
     */
    @PostMapping
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<UserDTO> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        //  Admin creates user in their own organization
        User user = userService.registerUserWithRole(
                request.email(),                    // email
                request.firstName(),                //  firstName
                request.lastName(),                 //  lastName
                request.password(),                 // password
                request.role(),                     // role
                currentUser.getOrganization()       // organization
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(UserDTO.from(user));
    }

    /**
     * Update user profile (username, email, firstName, lastName).
     *
     * ADMIN/MANAGER can update any user in their organization.
     * USER can only update their own profile.
     *
     * @param userId User UUID
     * @param request Profile update request
     * @return Updated user details
     */
    @PutMapping("/{userId}/profile")
    @PreAuthorize("hasAuthority('USER_MANAGE') or #userId == authentication.principal.id")
    public ResponseEntity<UserDTO> updateUserProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        User user = userService.updateUserProfile(
                userId,
                request.email(),
                request.firstName(),
                request.lastName()
        );
        return ResponseEntity.ok(UserDTO.from(user));
    }

    /**
     * Update user role.
     *
     * Only ADMIN can change user roles.
     *
     * @param userId User UUID
     * @param request Role update request
     * @return Updated user details
     */
    @PutMapping("/{userId}/role")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<UserDTO> updateUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        User user = userService.updateUserRole(userId, request.role());
        return ResponseEntity.ok(UserDTO.from(user));
    }

    /**
     * Change user password.
     *
     * ADMIN can change any user's password.
     * USER can only change their own password.
     *
     * @param userId User UUID
     * @param request Password change request
     * @return Empty response
     */
    @PutMapping("/{userId}/password")
    @PreAuthorize("hasAuthority('USER_MANAGE') or #userId == authentication.principal.id")
    public ResponseEntity<Void> changePassword(
            @PathVariable UUID userId,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changeUserPassword(userId, request.newPassword());
        return ResponseEntity.ok().build();
    }

    /**
     * Activate user account.
     *
     * Only ADMIN can activate users.
     *
     * @param id User UUID
     * @return Activated user details
     */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<UserDTO> activate(@PathVariable UUID id) {
        User user = userService.activate(id);
        return ResponseEntity.ok(UserDTO.from(user));
    }

    /**
     * Deactivate user account.
     *
     * Only ADMIN can deactivate users.
     *
     * @param id User UUID
     * @return Deactivated user details
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<UserDTO> deactivate(@PathVariable UUID id) {
        User user = userService.deactivate(id);
        return ResponseEntity.ok(UserDTO.from(user));
    }

    /**
     * Delete user (soft delete via BaseAuditedEntity).
     *
     * Only ADMIN can delete users.
     *
     * @param userId User UUID
     * @return Empty response
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Check if email is available for registration.
     *
     * @param email Email to check
     * @return Availability status
     */
    @GetMapping("/check-email")
    public ResponseEntity<AvailabilityResponse> checkEmailAvailability(@RequestParam String email) {
        boolean available = userService.isEmailAvailable(email);
        return ResponseEntity.ok(new AvailabilityResponse(available));
    }

    public record AvailabilityResponse(boolean available) {}
}