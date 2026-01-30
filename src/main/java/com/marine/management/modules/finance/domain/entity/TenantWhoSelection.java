package com.marine.management.modules.finance.domain.entity;

import com.marine.management.shared.domain.BaseTenantEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

import java.util.Objects;
import java.util.UUID;

/**
 * Tenant-specific WHO selection.
 *
 * TENANT ISOLATION:
 * - Extends BaseTenantEntity (auto tenant_id injection)
 * - Links tenant to global WHO
 * - Tenant can enable/disable WHO items
 */
@Entity
@Table(
        name = "tenant_who_selections",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_tenant_who",
                        columnNames = {"tenant_id", "who_id"}
                )
        },
        indexes = {
                @Index(name = "idx_tenant_who_tenant", columnList = "tenant_id"),
                @Index(name = "idx_tenant_who_who_id", columnList = "who_id"),
                @Index(name = "idx_tenant_who_enabled", columnList = "is_enabled")
        }
)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class TenantWhoSelection extends BaseTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    /**
     * Reference to global WHO.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "who_id", nullable = false)
    private Who who;

    /**
     * Tenant can disable a selected WHO.
     */
    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = true;  //  Field name without 'is' prefix

    protected TenantWhoSelection() {}

    // === FACTORY METHOD ===

    public static TenantWhoSelection create(Who who) {
        TenantWhoSelection selection = new TenantWhoSelection();
        selection.who = Objects.requireNonNull(who, "WHO cannot be null");
        selection.enabled = true;  //  Consistent field name

        return selection;
    }

    // === BUSINESS METHODS ===

    public void enable() {
        this.enabled = true;  //  Consistent field name
    }

    public void disable() {
        this.enabled = false;  //  Consistent field name
    }

    // === GETTERS ===

    public UUID getId() {
        return id;
    }

    public Who getWho() {
        return who;
    }

    public boolean isEnabled() {  //  Getter with 'is' prefix
        return enabled;
    }

    public void setEnabled(boolean enabled) {  //  Setter without 'is' prefix
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TenantWhoSelection that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
                "TenantWhoSelection{id=%s, tenantId=%s, whoCode='%s', enabled=%s}",
                id, getTenantId(),
                who != null ? who.getCode() : "null",
                enabled  //  Consistent field name
        );
    }
}