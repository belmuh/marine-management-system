package com.marine.management.shared.domain;

import com.marine.management.shared.domain.TenantEntityListener;
import jakarta.persistence.*;

/**
 * Base class for all tenant-isolated entities.
 *
 * DESIGN:
 * - Provides tenantId field with automatic population
 * - Abstract getId() allows flexible ID types (UUID, Long, etc.)
 * - TenantEntityListener auto-sets tenantId on persist
 * - @Filter applied in child entities for query filtering
 */
@MappedSuperclass
@EntityListeners(TenantEntityListener.class)
public abstract class BaseTenantEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    protected BaseTenantEntity() {}

    /**
     * Returns entity ID. Implementation depends on child class.
     * Used by TenantEntityListener for logging.
     */
    public abstract Object getId();

    // === TENANT MANAGEMENT ===

    void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public boolean belongsToTenant(Long tenantId) {
        if (this.tenantId == null) {
            throw new IllegalStateException("Entity not yet assigned to a tenant");
        }
        return this.tenantId.equals(tenantId);
    }
}