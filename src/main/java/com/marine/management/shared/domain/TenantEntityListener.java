package com.marine.management.shared.domain;

import com.marine.management.shared.multitenant.TenantContext;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA Entity Listener that enforces tenant isolation on entity lifecycle.
 *
 * LIFECYCLE HOOKS:
 * - @PrePersist: Injects tenant_id automatically
 * - @PreUpdate: Validates tenant_id hasn't changed
 *
 * SECURITY GUARANTEES:
 * 1. Automatic injection - impossible to forget tenant_id
 * 2. Immutability enforcement - prevents tenant_id changes
 * 3. Fails fast if no tenant context (prevents cross-tenant pollution)
 * 4. Validates entity type (only BaseTenantEntity processed)
 *
 * @see BaseTenantEntity
 * @see TenantContext
 */
public class TenantEntityListener {

    private static final Logger log = LoggerFactory.getLogger(TenantEntityListener.class);

    @PrePersist
    public void prePersist(Object entity) {
        if (!(entity instanceof BaseTenantEntity)) {
            log.trace("Entity {} is not a BaseTenantEntity, skipping tenant injection",
                    entity.getClass().getSimpleName());
            return;
        }

        BaseTenantEntity tenantEntity = (BaseTenantEntity) entity;

        if (!TenantContext.hasTenantContext()) {
            throw new IllegalStateException(
                    "Cannot persist tenant entity without active tenant context. " +
                            "Entity: " + entity.getClass().getSimpleName() + ". " +
                            "This is a critical security violation - tenant isolation would be compromised."
            );
        }

        Long tenantId = TenantContext.getCurrentTenantId();
        tenantEntity.setTenantId(tenantId);

        log.debug("Tenant ID injected into {}: tenantId={}",
                entity.getClass().getSimpleName(),
                tenantId);
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        if (!(entity instanceof BaseTenantEntity)) {
            return;
        }

        BaseTenantEntity tenantEntity = (BaseTenantEntity) entity;

        if (!TenantContext.hasTenantContext()) {
            throw new IllegalStateException(
                    "Cannot update tenant entity without active tenant context. " +
                            "Entity: " + entity.getClass().getSimpleName() +
                            ", ID: " + tenantEntity.getId() + ". " +  //  getId() çalışır (abstract method)
                            "This is a critical security violation - tenant isolation would be compromised."
            );
        }

        Long currentTenantId = TenantContext.getCurrentTenantId();
        Long entityTenantId = tenantEntity.getTenantId();

        if (entityTenantId == null) {
            log.error("SECURITY VIOLATION: Tenant ID is null on update. Entity: {}, ID: {}",
                    entity.getClass().getSimpleName(),
                    tenantEntity.getId());  //  getId() çalışır
            throw new IllegalStateException(
                    "Tenant ID cannot be null during update. " +
                            "Entity: " + entity.getClass().getSimpleName() +
                            ", ID: " + tenantEntity.getId()
            );
        }

        if (!entityTenantId.equals(currentTenantId)) {
            log.error("SECURITY VIOLATION: Attempt to change tenant_id or access wrong tenant's data. " +
                            "Entity: {}, ID: {}, Current Tenant: {}, Entity Tenant: {}",
                    entity.getClass().getSimpleName(),
                    tenantEntity.getId(),  //  getId() çalışır (UUID veya Long döner)
                    currentTenantId,
                    entityTenantId);
            throw new IllegalStateException(
                    "Tenant ID mismatch detected. " +
                            "Current tenant: " + currentTenantId + ", Entity tenant: " + entityTenantId + ". " +
                            "This is a critical security violation - cross-tenant data access prevented."
            );
        }

        log.trace("Tenant ID validation passed for {}: tenantId={}",
                entity.getClass().getSimpleName(),
                entityTenantId);
    }
}