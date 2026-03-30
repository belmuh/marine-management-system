package com.marine.management.modules.users.application;

import com.marine.management.modules.organization.domain.Organization;
import com.marine.management.modules.organization.infrastructure.OrganizationRepository;
import com.marine.management.modules.users.domain.User;
import com.marine.management.modules.users.infrastructure.UserRepository;
import com.marine.management.shared.security.Role;
import com.marine.management.shared.multitenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ============================================
    // TENANT CONTEXT
    // ============================================

    private Organization getCurrentTenantOrThrow() {
        if (!TenantContext.hasTenantContext()) {
            throw new AccessDeniedException(
                    "No tenant context available. User must be authenticated."
            );
        }

        Long tenantId = TenantContext.getCurrentTenantId();

        return organizationRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException(
                        "Tenant organization not found: " + tenantId
                ));
    }

    // ============================================
    // TENANT-SCOPED QUERIES
    // ============================================

    public List<User> getAllUsers() {
        Organization currentOrg = getCurrentTenantOrThrow();
        return userRepository.findByOrganization(currentOrg);
    }

    public Page<User> listUsersInCurrentOrganization(Pageable pageable) {
        Organization currentOrg = getCurrentTenantOrThrow();
        return userRepository.findByOrganization(currentOrg, pageable);
    }

    public Optional<User> findUserById(UUID id) {
        Organization currentOrg = getCurrentTenantOrThrow();
        return userRepository.findByIdAndOrganization(id, currentOrg);
    }

    public User getUserByIdOrThrow(UUID id) {
        return findUserById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found or does not belong to your organization: " + id
                ));
    }

    public long countUsersInCurrentOrganization() {
        Organization currentOrg = getCurrentTenantOrThrow();
        return userRepository.countByOrganization(currentOrg);
    }

    // ============================================
    // GLOBAL QUERIES (Authentication)
    // ============================================

    /**
     * Find user by email (for authentication).
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Check if email is available for registration.
     */
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    // ============================================
    // USER CREATION
    // ============================================

    /**
     * Register new user with default USER role.
     */
    @Transactional
    public User registerUser(
            String email,
            String firstName,
            String lastName,
            String plainPassword,
            Organization organization
    ) {
        return registerUserWithRole(
                email,
                firstName,
                lastName,
                plainPassword,
                Role.CREW,
                organization
        );
    }

    /**
     * Register new user with specific role.
     *
     * @param email Email (unique globally, used for login)
     * @param firstName First name
     * @param lastName Last name
     * @param plainPassword Plain text password (will be hashed)
     * @param role User role
     * @param organization Organization
     * @return Created user
     */
    @Transactional
    public User registerUserWithRole(
            String email,
            String firstName,
            String lastName,
            String plainPassword,
            Role role,
            Organization organization
    ) {
        // Validate email uniqueness
        if (userRepository.existsByEmail(email)) {
            log.warn("Failed to create user: email already exists: {}", email);
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        // Hash password
        String hashedPassword = passwordEncoder.encode(plainPassword);

        // Create user
        User user = User.createWithHashedPassword(
                email,
                firstName,
                lastName,
                hashedPassword,
                role,
                organization
        );

        User savedUser = userRepository.save(user);

        log.info("Created user: {} in organization: {}",
                savedUser.getId(),
                organization.getOrganizationId());

        return savedUser;
    }

    // ============================================
    // USER UPDATES
    // ============================================

    /**
     * Update user profile (email, firstName, lastName).
     *
     * CRITICAL: Changing email changes login credential!
     *
     * @param userId User ID
     * @param email New email
     * @param firstName New first name
     * @param lastName New last name
     * @return Updated user
     */
    @Transactional
    public User updateUserProfile(
            UUID userId,
            String email,
            String firstName,
            String lastName
    ) {
        User user = getUserByIdOrThrow(userId);

        // Validate email uniqueness (exclude current user)
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        // Log email change (security critical)
        if (!user.getEmail().equals(email)) {
            log.warn("User {} changing email from {} to {}",
                    user.getId(), user.getEmail(), email);
        }

        // Update profile
        user.updateProfile(email, firstName, lastName);

        return userRepository.save(user);
    }

    /**
     * Update user role (admin only).
     * Invalidates all existing refresh tokens so the user must re-login
     * and receive a new JWT reflecting the updated role.
     */
    @Transactional
    public User updateUserRole(UUID userId, Role newRole) {
        User user = getUserByIdOrThrow(userId);

        log.info("Changing role for user {} from {} to {}",
                user.getId(), user.getRole(), newRole);

        user.changeRole(newRole);
        User saved = userRepository.save(user);

        log.info("Role changed for user {} to {}", userId, newRole);

        return saved;
    }

    /**
     * Change user password.
     */
    @Transactional
    public void changeUserPassword(UUID userId, String newPassword) {
        User user = getUserByIdOrThrow(userId);

        String hashedPassword = passwordEncoder.encode(newPassword);
        user.changePassword(hashedPassword);

        userRepository.save(user);

        log.info("Password changed for user: {}", user.getId());
    }

    /**
     * Activate user account.
     */
    @Transactional
    public User activate(UUID userId) {
        User user = getUserByIdOrThrow(userId);
        user.activate();

        log.info("Activated user: {}", user.getId());
        return userRepository.save(user);
    }

    /**
     * Deactivate user account.
     */
    @Transactional
    public User deactivate(UUID userId) {
        User user = getUserByIdOrThrow(userId);
        user.deactivate();

        log.info("Deactivated user: {}", user.getId());
        return userRepository.save(user);
    }

    /**
     * Delete user (hard delete).
     */
    @Transactional
    public void deleteUser(UUID userId) {
        User user = getUserByIdOrThrow(userId);

        log.warn("Deleting user: {}", user.getId());
        userRepository.delete(user);
    }
}