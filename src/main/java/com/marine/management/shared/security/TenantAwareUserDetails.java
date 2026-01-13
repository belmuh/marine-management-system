package com.marine.management.shared.security;


/**
 * Extension of Spring Security UserDetails for multi-tenant systems.
 *
 * ARCHITECTURE:
 * - Decouples JWT/TenantFilter from domain User entity
 * - Polymorphic support for multiple user implementations
 * - Clean Architecture boundary
 */
public interface TenantAwareUserDetails {

    /**
     * Returns tenant ID for tenant isolation.
     *
     * @return tenant ID, or null if user has no tenant (e.g., system admin)
     */
    Long getTenantId();

    /**
     * Returns user's role as string for JWT claim.
     *
     * @return role name (e.g., "ADMIN", "USER")
     */
    String getRole();
}
