// modules/users/infrastructure/UserRepository.java
package com.marine.management.modules.users.infrastructure;

import com.marine.management.modules.organization.domain.Organization;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.security.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // ============================================
    // GLOBAL QUERIES (Authentication)
    // ============================================

    /**
     * Find user by email (global - for authentication).
     * Email is unique across the entire system.
     */
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    /**
     * Find user by email with eager-loaded organization.
     * Useful for authentication flow to avoid N+1 queries.
     */
    @Query("SELECT u FROM User u JOIN FETCH u.organization WHERE u.email = :email")
    Optional<User> findByEmailWithOrganization(@Param("email") String email);

    /**
     * Find user by verification token (used for email verification).
     */
    Optional<User> findByVerificationToken(String verificationToken);

    /**
     * Find user by password reset token (used for password reset flow).
     * Global lookup — reset tokens are unique across the system.
     */
    Optional<User> findByPasswordResetToken(String passwordResetToken);

    // ============================================
    // TENANT-SCOPED QUERIES
    // ============================================

    /**
     * Find user by ID within specific organization (tenant isolation).
     *
     * BETTER THAN: findById().filter(user -> user.getOrganization().equals(org))
     * - Makes intent explicit in repository
     * - Single database query (more efficient)
     * - Clear tenant isolation semantics
     */
    Optional<User> findByIdAndOrganization(UUID id, Organization organization);

    /**
     * Find user by email within specific organization.
     * Useful for tenant-scoped operations where you need to verify
     * the user belongs to the current tenant.
     */
    Optional<User> findByEmailAndOrganization(String email, Organization organization);

    /**
     * Find user by email and organization ID.
     * Alternative to passing Organization entity.
     */
    Optional<User> findByEmailAndOrganization_Id(String email, Long organizationId);

    /**
     * Find all users in organization.
     */
    List<User> findByOrganization(Organization organization);

    /**
     * Find all users in organization (paginated).
     */
    Page<User> findByOrganization(Organization organization, Pageable pageable);

    /**
     * Find only active users in organization.
     */
    List<User> findByOrganizationAndIsActiveTrue(Organization organization);

    @Query("""
        SELECT u.id, CONCAT(u.firstName, ' ', u.lastName)
        FROM User u
        WHERE u.id IN :ids
    """)
    List<Object[]> findNamesByIdsRaw(@Param("ids") Set<UUID> ids);

    // Default method ile Map'e çevir:
    default Map<UUID, String> findNamesByIds(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return findNamesByIdsRaw(ids).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (String) row[1]
                ));
    }

    /**
     * Find users by role within organization.
     */
    @Query("SELECT u FROM User u WHERE u.organization = :organization AND u.role = :role")
    List<User> findByOrganizationAndRole(@Param("organization") Organization organization,
                                         @Param("role") Role role);


    long countByOrganization(Organization organization);
    long countByOrganizationAndIsActiveTrue(Organization organization);

    @Query("SELECT u FROM User u WHERE u.organization = :organization AND u.role = 'ADMIN'")
    List<User> findAdminsByOrganization(@Param("organization") Organization organization);

    boolean existsByIdAndOrganization(UUID id, Organization organization);
    boolean existsByEmailAndOrganization(String email, Organization organization);
}