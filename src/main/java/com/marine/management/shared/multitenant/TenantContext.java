package com.marine.management.shared.multitenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-local storage for current tenant ID.
 *
 * DESIGN DECISION: Store tenant ID (Long), not Organization entity.
 *
 * WHY ID-ONLY?
 * - Lighter memory footprint (8 bytes vs entity proxy)
 * - No Hibernate proxy issues (lazy-load, detached entity)
 * - Async propagation simpler (primitive value)
 * - No accidental entity modifications
 * - Thread-safe by design
 *
 * @see TenantFilter
 * @see TenantAwareTaskDecorator
 */
public final class TenantContext {

    private static final Logger log = LoggerFactory.getLogger(TenantContext.class);

    private static final ThreadLocal<Long> CURRENT_TENANT_ID = new ThreadLocal<>();

    private TenantContext() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Sets the current tenant ID for this thread.
     *
     * @param tenantId the tenant ID
     * @throws IllegalArgumentException if tenantId is null
     */
    public static void setCurrentTenantId(Long tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }

        CURRENT_TENANT_ID.set(tenantId);
        log.debug("Tenant context set: tenantId={}", tenantId);
    }

    /**
     * Gets the current tenant ID for this thread.
     *
     * @return the current tenant ID
     * @throws TenantContextException if no tenant context is available
     */
    public static Long getCurrentTenantId() {
        Long tenantId = CURRENT_TENANT_ID.get();

        if (tenantId == null) {
            throw new TenantContextException(
                    "No tenant context available. User must be authenticated."
            );
        }

        return tenantId;
    }

    /**
     * Checks if a tenant context is currently set.
     */
    public static boolean hasTenantContext() {
        return CURRENT_TENANT_ID.get() != null;
    }

    /**
     * Clears the tenant context for this thread.
     *
     * CRITICAL: Must be called in finally block.
     */
    public static void clear() {
        Long tenantId = CURRENT_TENANT_ID.get();

        if (tenantId != null) {
            log.trace("Clearing tenant context: tenantId={}", tenantId);
        }

        CURRENT_TENANT_ID.remove();
    }

    public static class TenantContextException extends RuntimeException {
        public TenantContextException(String message) {
            super(message);
        }
    }
}