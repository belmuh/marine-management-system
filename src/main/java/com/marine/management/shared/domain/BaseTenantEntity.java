package com.marine.management.shared.domain;

import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.multitenant.TenantContext;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

/**
 * Base for tenant-isolated entities.
 * Extends BaseAuditedEntity and adds tenant isolation.
 */
@MappedSuperclass
@EntityListeners(TenantEntityListener.class)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public abstract class BaseTenantEntity extends BaseAuditedEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    protected BaseTenantEntity() {}

    void setTenantId(Long tenantId) {
        if (this.tenantId != null) {
            throw new IllegalStateException("Tenant ID cannot be changed");
        }
        this.tenantId = tenantId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public boolean doesNotBelongToTenant(Long tenantId) {
        if (this.tenantId == null) {
            throw new IllegalStateException("Entity not assigned to tenant");
        }
        return !this.tenantId.equals(tenantId);
    }

    private void validateTenantContext() {
        Long currentTenantId = TenantContext.getCurrentTenantId();
        if (currentTenantId == null) {
            throw new IllegalStateException("No tenant context");
        }
        if (doesNotBelongToTenant(currentTenantId)) {
            throw new IllegalStateException("Tenant mismatch");
        }
    }

    public void softDelete(User deletedBy) {
        validateTenantContext();
        super.softDelete(deletedBy.getUserId());
    }

    public void restore(User restoredBy) {
        validateTenantContext();
        super.restore(restoredBy.getUserId());
    }
}