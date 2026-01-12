package com.marine.management.modules.users.infrastructure;

import com.marine.management.modules.organization.domain.Organization;
import com.marine.management.modules.users.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // ============================================
    // GLOBAL QUERIES (Authentication)
    // ============================================

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // ============================================
    // TENANT-SCOPED QUERIES
    // ============================================

    /**
     * Find user by ID within specific organization.
     *
     * BETTER THAN: findById().filter(user -> ...)
     * - Makes intent explicit in repository
     * - Single database query (more efficient)
     * - Clear tenant isolation semantics
     */
    Optional<User> findByIdAndOrganization(UUID id, Organization organization);

    Optional<User> findByUsernameAndOrganization(String username, Organization organization);
    Optional<User> findByEmailAndOrganization(String email, Organization organization);

    List<User> findByOrganization(Organization organization);
    Page<User> findByOrganization(Organization organization, Pageable pageable);
    List<User> findByOrganizationAndIsActiveTrue(Organization organization);

    long countByOrganization(Organization organization);
    long countByOrganizationAndIsActiveTrue(Organization organization);
}