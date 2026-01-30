package com.marine.management.shared.domain;

import com.marine.management.modules.users.domain.User;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * JPA listener that sets audit fields from SecurityContext.
 */
public class AuditingEntityListener {

    @PrePersist
    public void setCreatedBy(BaseAuditedEntity entity) {
        UUID userId = getCurrentUserId();
        if (userId != null) {
            entity.setCreatedBy(userId);
            entity.setUpdatedBy(userId);
        }
    }

    @PreUpdate
    public void setUpdatedBy(BaseAuditedEntity entity) {
        UUID userId = getCurrentUserId();
        if (userId != null) {
            entity.setUpdatedBy(userId);
        }
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof User user) {
            return user.getUserId();
        }

        return null;
    }
}