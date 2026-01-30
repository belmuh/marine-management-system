// modules/users/domain/User.java
package com.marine.management.modules.users.domain;

import com.marine.management.modules.organization.domain.Organization;
import com.marine.management.shared.domain.BaseAuditedEntity;
import com.marine.management.shared.security.Role;
import com.marine.management.shared.security.TenantAwareUserDetails;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_users_email", columnNames = "email")
        },
        indexes = {
                @Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_organization", columnList = "organization_id")
        }
)
public class User extends BaseAuditedEntity implements UserDetails, TenantAwareUserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    // ⭐ SINGLE SOURCE OF TRUTH: email
    @Column(unique = true, nullable = false, length = 100)
    @Email
    private String email;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "password_hash", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    protected User() {}

    private User(String email, String password, Role role, Organization organization) {
        this.email = validateEmail(email);
        this.password = Objects.requireNonNull(password, "Password cannot be null");
        this.role = role;
        this.organization = Objects.requireNonNull(organization, "Organization cannot be null");
    }

    // ⭐ FACTORY METHODS
    public static User create(String email, String plainPassword, Organization organization) {
        return new User(email, plainPassword, Role.USER, organization);
    }

    public static User createWithRole(String email, String plainPassword, Role role, Organization organization) {
        return new User(email, plainPassword, role, organization);
    }

    public static User createWithHashedPassword(
            String email,
            String firstName,
            String lastName,
            String hashedPassword,
            Role role,
            Organization organization
    ) {
        User user = new User(email, hashedPassword, role, organization);
        user.firstName = firstName;
        user.lastName = lastName;
        return user;
    }

    // Called on successful login
    public void updateLastLogin(LocalDateTime loginTime) {
        this.lastLoginAt = loginTime;
    }

    // UserDetails implementation - email is username for Spring Security
    @Override
    public String getUsername() {
        return email;  // Spring Security uses this for authentication
    }

    @Override
    public String getPassword() {
        return password;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }

    // ⭐ TenantAwareUserDetails implementation
    @Override
    public Long getTenantId() {
        return getOrganizationId();
    }

    @Override
    public String getRole() {
        return role.name();
    }

    @Override
    public Object getId() {
        return id;
    }

    // ⭐ BUSINESS METHODS
    public UUID getUserId() {
        return id;
    }

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

    public void updateProfile(String newEmail, String newFirstName, String newLastName) {
        this.email = validateEmail(newEmail);
        this.firstName = newFirstName;
        this.lastName = newLastName;
    }

    public void changePassword(String newPassword) {
        this.password = Objects.requireNonNull(newPassword, "Password cannot be null");
    }

    public void changeRole(Role newRole) {
        this.role = Objects.requireNonNull(newRole, "Role cannot be null");
    }

    // ⭐ VALIDATION
    private String validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        return email.trim().toLowerCase();
    }

    // ⭐ GETTERS
    public Organization getOrganization() {
        return organization;
    }

    public Long getOrganizationId() {
        return organization != null ? organization.getOrganizationId() : null;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        if (firstName == null && lastName == null) return email;
        if (firstName != null && lastName != null) return firstName + " " + lastName;
        return firstName != null ? firstName : lastName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

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
        return String.format("User{id=%s, email='%s', role=%s}",
                id, email, role);
    }
}