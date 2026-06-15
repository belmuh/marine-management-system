package com.marine.management.shared.domain;

import jakarta.persistence.*;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.DefaultRevisionEntity;

import java.util.UUID;

/**
 * Custom Envers revision entity that captures WHO made the change and HOW.
 *
 * Extends DefaultRevisionEntity (provides rev number + timestamp).
 * Adds user identity and change context for debugging and display.
 *
 * Fields:
 * - userId, username, userDisplayName → who made the change
 * - source → how (API, SYSTEM, BATCH) — useful for distinguishing
 *   user-initiated vs automated changes
 * - correlationId → trace ID for debugging across services
 *
 * Why userDisplayName?
 * Avoids extra JOIN to users table when rendering history in frontend.
 * User might be renamed/deleted later — displayName is a snapshot.
 */
@Entity
@Table(name = "revinfo", indexes = {
        @Index(name = "idx_revinfo_user_id", columnList = "user_id"),
        @Index(name = "idx_revinfo_timestamp", columnList = "revtstmp")
})
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "rev")),
        @AttributeOverride(name = "timestamp", column = @Column(name = "revtstmp"))
})
@RevisionEntity(CustomRevisionListener.class)
public class CustomRevisionEntity extends DefaultRevisionEntity {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "user_display_name", length = 200)
    private String userDisplayName;

    /**
     * Source of the change: API (user request), SYSTEM (scheduler/listener), BATCH (bulk operation)
     */
    @Column(name = "source", length = 20, nullable = false)
    private String source = "API";

    /**
     * Correlation ID for distributed tracing / debugging.
     * Typically matches MDC requestId or X-Correlation-Id header.
     */
    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    // ─── Getters & Setters ───

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getUserDisplayName() { return userDisplayName; }
    public void setUserDisplayName(String userDisplayName) { this.userDisplayName = userDisplayName; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}
