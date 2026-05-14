package com.marine.management.shared.domain;

import com.marine.management.modules.users.domain.User;
import org.hibernate.envers.RevisionListener;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Envers revision listener that populates CustomRevisionEntity
 * from SecurityContext and MDC.
 *
 * Called by Envers on every revision (before persisting revinfo row).
 * Not a Spring bean — instantiated by Hibernate directly.
 */
public class CustomRevisionListener implements RevisionListener {

    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final String MDC_SOURCE = "auditSource";

    @Override
    public void newRevision(Object revisionEntity) {
        CustomRevisionEntity rev = (CustomRevisionEntity) revisionEntity;

        populateUserInfo(rev);
        populateContext(rev);
    }

    private void populateUserInfo(CustomRevisionEntity rev) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            rev.setUsername("SYSTEM");
            rev.setUserDisplayName("System");
            rev.setSource("SYSTEM");
            return;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof User user) {
            rev.setUserId(user.getUserId());
            rev.setUsername(user.getUsername());
            rev.setUserDisplayName(user.getFullName());
        }
    }

    private void populateContext(CustomRevisionEntity rev) {
        // Correlation ID from MDC (set by request filter or batch job)
        String correlationId = MDC.get(MDC_CORRELATION_ID);
        if (correlationId != null) {
            rev.setCorrelationId(correlationId);
        }

        // Source override from MDC (batch jobs can set this)
        String source = MDC.get(MDC_SOURCE);
        if (source != null) {
            rev.setSource(source);
        }
    }
}
