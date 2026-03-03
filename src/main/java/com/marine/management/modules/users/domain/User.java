package com.marine.management.modules.users.domain;

import com.marine.management.modules.organization.domain.Organization;
import com.marine.management.shared.domain.BaseAuditedEntity;
import com.marine.management.shared.security.Permission;
import com.marine.management.shared.security.Role;
import com.marine.management.shared.security.TenantAwareUserDetails;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

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

    @Column(unique = true, nullable = false, length = 100)
    @Email
    private String email;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "password_hash", nullable = false, length = 60)  // 👈 BCrypt needs 60
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.CREW;  // 👈 CHANGED: USER → CREW

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

    // FACTORY METHODS
    public static User create(String email, String plainPassword, Organization organization) {
        return new User(email, plainPassword, Role.CREW, organization);
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

    public void updateLastLogin(LocalDateTime loginTime) {
        this.lastLoginAt = loginTime;
    }

    // UserDetails implementation
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();

        System.out.println("=== DEBUG getAuthorities ===");
        System.out.println("User: " + this.email);
        System.out.println("Role field: " + this.role);

        if (this.role == null) {
            System.out.println("WARNING: Role is NULL!");
            return authorities;
        }

        // 1. Role authority (ROLE_ prefix for Spring Security hasRole() check)
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));

        Set<Permission> permissions = role.getAllPermissions();
        System.out.println("Permissions count: " + permissions.size());
        System.out.println("Permissions: " + permissions);

        // 2. Permission authorities (without prefix for hasAuthority() check)
        role.getAllPermissions().forEach(permission ->
                authorities.add(new SimpleGrantedAuthority(permission.name()))
        );

        System.out.println("Total authorities: " + authorities);
        System.out.println("=== END DEBUG ===");

        return authorities;
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

    // TenantAwareUserDetails implementation
    @Override
    public Long getTenantId() {
        return getOrganizationId();
    }

    @Override
    public String getRole() {  // Interface requirement (String)
        return role.name();
    }

    @Override
    public Object getId() {
        return id;
    }

    // BUSINESS METHODS
    public UUID getUserId() {
        return id;
    }

    //  NEW: Get role as enum
    public Role getRoleEnum() {
        return role;
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


    // Existing permissions
    public boolean isAdmin() {
        return role.isAdmin();
    }

    public boolean isSuperAdmin() {
        return role.isSuperAdmin();
    }

    public boolean isCrew() {
        return role.isCrew();
    }

    public boolean isCaptain() {
        return role.isCaptain();
    }

    public boolean isManager() {
        return role.isManager();
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

    private String validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        return email.trim().toLowerCase();
    }

    // GETTERS
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

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
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