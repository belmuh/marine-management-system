package com.marine.management.modules.users.domain;

import com.marine.management.modules.organization.domain.Organization;
import com.marine.management.shared.security.Role;
import com.marine.management.shared.security.TenantAwareUserDetails;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * User entity implementing Spring Security UserDetails.
 *
 * WHY UserDetails?
 * - Standard Spring Security interface
 * - Clean integration with authentication system
 * - No need for separate UserDetailsService adapter
 * - Domain entity carries its own security context
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_users_username", columnNames = "username"),
                @UniqueConstraint(name = "uq_users_email", columnNames = "email")
        },
        indexes = {
                @Index(name = "idx_users_username", columnList = "username"),
                @Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_organization", columnList = "organization_id")
        }
)
public class User implements UserDetails, TenantAwareUserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    protected User() {}

    private User(String username, String email, String password, Role role, Organization organization) {
        this.username = validateUsername(username);
        this.email = validateEmail(email);
        this.password = Objects.requireNonNull(password, "Password cannot be null");
        this.role = role;
        this.organization = Objects.requireNonNull(organization, "Organization cannot be null");
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // === FACTORY METHODS ===

    public static User create(String username, String email, String plainPassword, Organization organization) {
        return new User(username, email, plainPassword, Role.USER, organization);
    }

    public static User createWithRole(String username, String email, String plainPassword, Role role, Organization organization) {
        return new User(username, email, plainPassword, role, organization);
    }

    public static User createWithHashedPassword(String username, String email, String hashedPassword, Role role, Organization organization) {
        return new User(username, email, hashedPassword, role, organization);
    }

    // === UserDetails IMPLEMENTATION ===

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Return role as authority
        // Spring Security expects "ROLE_" prefix for hasRole() checks
        // But we use hasAuthority() so no prefix needed
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;  // We don't track account expiration
    }

    @Override
    public boolean isAccountNonLocked() {
        return isActive;  // Locked = !isActive
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;  // We don't track credential expiration
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }

    // === BUSINESS METHODS ===

    public boolean isActive() {
        return isActive;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public boolean canManageFinancialEntries() {
        return role.canCreateEntry();
    }

    public boolean canViewAllEntries() {
        return role.canViewAllEntries();
    }

    public boolean canEditAnyEntry() {
        return role.canEditAnyEntry();
    }

    public boolean canDeleteEntry() {
        return role.canDeleteEntry();
    }

    public boolean canViewReports() {
        return role.canViewReports();
    }

    public boolean canViewBudgets() {
        return role.canViewBudgets();
    }

    public boolean isAdmin() {
        return role.isAdmin();
    }

    public boolean isSuperAdmin() {
        return role.isSuperAdmin();
    }

    public boolean credentialsMatch(String inputPassword, PasswordEncoder encoder) {
        return encoder.matches(inputPassword, this.password);
    }

    public void updateProfile(String newUsername, String newEmail) {
        this.username = validateUsername(newUsername);
        this.email = validateEmail(newEmail);
    }

    public void changePassword(String newPassword) {
        this.password = Objects.requireNonNull(newPassword, "Password cannot be null");
    }

    public void changeRole(Role newRole) {
        this.role = Objects.requireNonNull(newRole, "Role cannot be null");
    }

    // === VALIDATION ===

    private String validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (username.length() < 3 || username.length() > 50) {
            throw new IllegalArgumentException("Username must be between 3 and 50 characters");
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Username can only contain letters, numbers and underscores");
        }
        return username.trim();
    }

    private String validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        return email.trim().toLowerCase();
    }

    // === GETTERS ===

    public UUID getId() { return id; }
    public Organization getOrganization() { return organization; }
    public Long getOrganizationId() {
        return organization != null ? organization.getId() : null;
    }
    // getUsername() from UserDetails
    public String getEmail() { return email; }
    @Override
    public Long getTenantId() {
        return getOrganizationId();
    }

    @Override
    public String getRole() {
        return role.name();
    }

    // isActive() as isEnabled() from UserDetails
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }

    public String getFullName() {
        if (firstName == null && lastName == null) return username;
        if (firstName != null && lastName != null) return firstName + " " + lastName;
        return firstName != null ? firstName : lastName;
    }

    // getPassword() from UserDetails
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("User{id=%s, username='%s', email='%s', role=%s}",
                id, username, email, role);
    }
}