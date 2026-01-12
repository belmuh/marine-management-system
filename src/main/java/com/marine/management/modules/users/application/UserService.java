package com.marine.management.modules.users.application;

import com.marine.management.modules.organization.domain.Organization;
import com.marine.management.modules.organization.infrastructure.OrganizationRepository;
import com.marine.management.modules.users.domain.User;
import com.marine.management.modules.users.infrastructure.UserRepository;
import com.marine.management.shared.security.Role;
import com.marine.management.shared.multitenant.TenantContext;
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

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    //  Constructor updated
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
    // TENANT-AWARE QUERIES (Use TenantContext)
    // ============================================

    /**
     * Gets current tenant with guard clause.
     *
     * CRITICAL: Fails fast if no tenant context.
     * Better than NPE deep in the code.
     */
    private Organization getCurrentTenantOrThrow() {
        if (!TenantContext.hasTenantContext()) {
            throw new AccessDeniedException(
                    "No tenant context available. User must be authenticated."
            );
        }

        Long tenantId = TenantContext.getCurrentTenantId();  // ✅ FIXED: getCurrentTenant() → getCurrentTenantId()

        return organizationRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found: " + tenantId));
    }

    /**
     * List all users in current tenant's organization.
     */
    public List<User> getAllUsers() {  // ✅ RENAMED: Controller expects getAllUsers()
        Organization currentOrg = getCurrentTenantOrThrow();
        return userRepository.findByOrganization(currentOrg);
    }

    /**
     * List users with pagination in current organization.
     */
    public Page<User> listUsersInCurrentOrganization(Pageable pageable) {
        Organization currentOrg = getCurrentTenantOrThrow();
        return userRepository.findByOrganization(currentOrg, pageable);
    }

    /**
     * Find user by ID in current organization.
     *
     * IMPROVED: Uses explicit repository method instead of filter.
     * - Single query (more efficient)
     * - Clear intent
     * - Database-level guarantee
     */
    public Optional<User> findUserById(UUID id) {
        Organization currentOrg = getCurrentTenantOrThrow();
        return userRepository.findByIdAndOrganization(id, currentOrg);
    }

    /**
     * Find user by ID or throw exception.
     */
    public User getUserByIdOrThrow(UUID id) {  // ✅ RENAMED: Controller expects getUserByIdOrThrow()
        return findUserById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found or does not belong to your organization: " + id
                ));
    }

    /**
     * Count users in current organization.
     */
    public long countUsersInCurrentOrganization() {
        Organization currentOrg = getCurrentTenantOrThrow();
        return userRepository.countByOrganization(currentOrg);
    }

    // ============================================
    // GLOBAL QUERIES (No tenant filter)
    // ============================================

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    // ✅ NEW: Controller expects these methods
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    // ============================================
    // WRITE OPERATIONS
    // ============================================

    @Transactional
    public User registerUser(String username, String email, String plainPassword, Organization organization) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        String hashedPassword = passwordEncoder.encode(plainPassword);
        User user = User.createWithHashedPassword(username, email, hashedPassword, Role.USER, organization);

        return userRepository.save(user);
    }

    @Transactional
    public User registerUserWithRole(
            String username,
            String email,
            String plainPassword,
            Role role,
            Organization organization
    ) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        String hashedPassword = passwordEncoder.encode(plainPassword);
        User user = User.createWithHashedPassword(username, email, hashedPassword, role, organization);

        return userRepository.save(user);
    }

    // ✅ NEW: Controller expects these methods
    @Transactional
    public User updateUserProfile(UUID userId, String username, String email) {
        User user = getUserByIdOrThrow(userId);

        // Check username uniqueness (exclude current user)
        if (!user.getUsername().equals(username) && userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Check email uniqueness (exclude current user)
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        user.updateProfile(username, email);
        return userRepository.save(user);
    }

    @Transactional
    public User updateUserRole(UUID userId, Role newRole) {
        User user = getUserByIdOrThrow(userId);
        user.changeRole(newRole);
        return userRepository.save(user);
    }

    @Transactional
    public void changeUserPassword(UUID userId, String newPassword) {
        User user = getUserByIdOrThrow(userId);
        String hashedPassword = passwordEncoder.encode(newPassword);
        user.changePassword(hashedPassword);
        userRepository.save(user);
    }

    @Transactional
    public User activate(UUID userId) {
        User user = getUserByIdOrThrow(userId);
        user.activate();
        return userRepository.save(user);
    }

    @Transactional
    public User deactivate(UUID userId) {
        User user = getUserByIdOrThrow(userId);
        user.deactivate();
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = getUserByIdOrThrow(userId);
        userRepository.delete(user);
    }
}