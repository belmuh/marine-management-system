package com.marine.management.shared.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base for all entities requiring audit trail.
 * No tenant isolation - used for global entities (User, Organization).
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Where(clause = "is_deleted = false")
public abstract class BaseAuditedEntity {

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by_id")
    private UUID deletedById;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by_id", updatable = false)
    private UUID createdById;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by_id")
    private UUID updatedById;

    protected BaseAuditedEntity() {}

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public abstract Object getId();

    void setCreatedBy(UUID userId) {
        if (this.createdById != null) {
            throw new IllegalStateException("Creator cannot be changed");
        }
        this.createdById = userId;
    }

    void setUpdatedBy(UUID userId) {
        this.updatedById = userId;
    }

    public void softDelete(UUID deletedBy) {
        if (this.deleted) {
            throw new IllegalStateException("Entity already deleted");
        }
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedById = deletedBy;
        this.updatedAt = LocalDateTime.now();
        this.updatedById = deletedBy;
    }

    public void restore(UUID restoredBy) {
        if (!this.deleted) {
            throw new IllegalStateException("Entity not deleted");
        }
        this.deleted = false;
        this.deletedAt = null;
        this.deletedById = null;
        this.updatedAt = LocalDateTime.now();
        this.updatedById = restoredBy;
    }

    public Long getVersion() { return version; }
    public boolean isDeleted() { return deleted; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public UUID getDeletedById() { return deletedById; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public UUID getCreatedById() { return createdById; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public UUID getUpdatedById() { return updatedById; }
    public boolean isActive() { return !deleted; }
}