package com.marine.management.modules.users.domain;

import com.marine.management.shared.kernel.security.Role;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;



@Entity
@Table(name="users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID DEFAULT gen_random_uuid()", updatable = false, nullable = false)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    protected User(){}

    private User(String username, String email, String password, Role role){
        this.username = validateUsername(username);
        this.email = validateEmail(email);
        this.password = Objects.requireNonNull(password, "Password cannot be null");
        this.role = role;
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

    // === FACTORY METHOD (Static Factory Pattern) ===

    public static User create(String username, String email, String plainPassword) {
        return new User(username, email, plainPassword, Role.USER);
    }

    public static User createWithRole(String username, String email, String plainPassword, Role role) {
        return new User(username, email, plainPassword, role);
    }

    public static User createWithHashedPassword(String username, String email, String hashedPassword, Role role) {
        return new User(username, email, hashedPassword, role);
    }
    // === DOMAIN BUSINESS METHODS === (DDD Lite)

    public boolean canManageFinancialEntries() {
        return role.canManageFinancialEntries();
    }

    public boolean canViewReports() {
        return role.canViewReports();
    }

    public boolean isAdmin() {
        return role.isAdmin();
    }

    public boolean credentialsMatch(String inputPassword){
        return this.password.equals(inputPassword);
    }

    public void updateProfile(String newUsername, String newEmail){
        this.username = validateUsername(newUsername);
        this.email = validateEmail(newEmail);
    }

    public void changePassword(String newPassword){
        this.password = Objects.requireNonNull(newPassword, "Password cannot be null");
    }

    public void changeRole(Role newRole) {
        this.role = Objects.requireNonNull(newRole, "Role cannot be null");
    }

    // === PRIVATE VALIDATION METHODS === (Clean Code)

    private String validateUsername(String username){
        if(username == null || username.trim().isEmpty()){
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if(username.length() < 3 || username.length() > 50){
            throw new IllegalArgumentException("Username must be between 3 and 50 characters");
        }
        if(!username.matches("^[a-zA-Z0-9_]+$")){
            throw new IllegalArgumentException("username can only contain letters, numbers and underscores");
        }
        return username.trim();
    }

    private String validateEmail(String email){
        if(email == null || email.trim().isEmpty()){
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if(!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")){
            throw new IllegalArgumentException("Invalid email format");
        }
        return email.trim().toLowerCase();
    }

    // === GETTERS ===

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    String getPassword() {
        return password;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // === EQUALS & HASHCODE ===

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

    // === TO STRING ===

    @Override
    public String toString() {
        return String.format("User{id=%s, username='%s', email='%s', role=%s}",
                id, username, email, role);
    }


}
