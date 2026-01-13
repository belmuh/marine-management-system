package com.marine.management.shared.multitenant;

import com.marine.management.modules.auth.infrastructure.JwtUtil;
import com.marine.management.modules.users.domain.User;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter for tenant context and Hibernate filter management.
 *
 * INFRASTRUCTURE PURITY:
 * - Uses EntityManagerFactory (not EntityManager)
 * - Lazy session unwrap (only when needed)
 * - No direct JPA lifecycle coupling
 *
 * RESPONSIBILITIES:
 * 1. Extract tenant from authenticated user
 * 2. Set TenantContext (ThreadLocal)
 * 3. Enable Hibernate tenant filter (when session available)
 * 4. Clear TenantContext on request completion
 *
 * SAFETY GUARANTEES:
 * - Filter enable is best-effort (session may not exist yet)
 * - TenantEntityListener provides fallback (PrePersist/PreUpdate)
 * - Metrics track filter operations for observability
 *
 * @see TenantContext
 * @see TenantInterceptor
 * @see com.marine.management.shared.domain.TenantEntityListener
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);
    private static final String TENANT_FILTER_NAME = "tenantFilter";

    private final SessionFactory sessionFactory;
    private final TenantFilterMetrics metrics;
    private final JwtUtil jwtUtil;

    public TenantFilter(
            EntityManagerFactory entityManagerFactory,
            TenantFilterMetrics metrics,
            JwtUtil jwtUtil
    ) {
        this.sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        this.metrics = metrics;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            boolean tenantEstablished = establishTenantContext();

            if (tenantEstablished) {
                // ✅ DEBUG LOG EKLE
                log.debug("🔵 Tenant BEFORE chain: tenantId={}, path={}",
                        TenantContext.getCurrentTenantId(), request.getRequestURI());

                enableHibernateTenantFilter();
            }

            filterChain.doFilter(request, response);

            // ✅ DEBUG LOG EKLE
            if (tenantEstablished) {
                log.debug("🟢 Tenant AFTER chain (before clear): tenantId={}",
                        TenantContext.getCurrentTenantId());
            }

        } finally {
            // ✅ DEBUG LOG EKLE
            Long tenantBeforeClear = TenantContext.getCurrentTenantId();
            if (tenantBeforeClear != null) {
                log.debug("🔴 Tenant CLEARING: was={}", tenantBeforeClear);
            }

            clearTenantContext();

            log.trace("Tenant context cleared after request completion");
        }
    }

    /**
     * Establishes tenant context from authenticated user.
     *
     * @return true if tenant context was established, false otherwise
     */
    private boolean establishTenantContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.trace("No authenticated user, skipping tenant context setup");
            return false;
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof User)) {
            log.trace("Principal is not a User instance: {}",
                    principal != null ? principal.getClass().getSimpleName() : "null");
            return false;
        }

        User user = (User) principal;

        if (user.getOrganization() == null) {
            log.warn("User {} has no organization assigned", user.getUsername());
            return false;
        }

        // Store tenant ID only (not entity - lighter, no proxy issues)
        TenantContext.setCurrentTenantId(user.getOrganization().getId());

        log.debug("Tenant context established: user={}, tenantId={}",
                user.getUsername(),
                user.getOrganization().getId());

        return true;
    }

    /**
     * Enables Hibernate tenant filter with verification.
     *
     * BEST-EFFORT APPROACH:
     * - Session may not be available yet (not an error)
     * - Filter will be applied on first query if session exists
     * - TenantEntityListener provides additional safety layer
     *
     * @return true if filter was enabled or deferred, false only on critical failure
     */
    private boolean enableHibernateTenantFilter() {
        try {
            Session session = sessionFactory.getCurrentSession();

            // GUARD: Session may not be opened yet (deferred initialization)
            if (session == null || !session.isOpen()) {
                log.debug("Hibernate session not available yet, filter will apply on first DB access");
                metrics.recordFilterEnableSuccess(); // Not a failure - expected behavior
                return true;
            }

            Filter existingFilter = session.getEnabledFilter(TENANT_FILTER_NAME);

            if (existingFilter != null) {
                log.trace("Tenant filter already enabled");
                metrics.recordFilterEnableSuccess();
                return true;
            }

            Long tenantId = TenantContext.getCurrentTenantId();

            session.enableFilter(TENANT_FILTER_NAME)
                    .setParameter("tenantId", tenantId);

            // Verify filter was actually enabled
            Filter verifyFilter = session.getEnabledFilter(TENANT_FILTER_NAME);
            if (verifyFilter == null) {
                log.error("CRITICAL: Filter enable failed verification! TenantId: {}", tenantId);
                metrics.recordFilterEnableFailure();
                return false;
            }

            log.debug("Hibernate tenant filter enabled: tenantId={}", tenantId);
            metrics.recordFilterEnableSuccess();
            return true;

        } catch (IllegalStateException e) {
            // Session not bound to thread yet - OK, filter will apply on first query
            log.debug("Session not bound yet, filter will be applied on first DB access: {}", e.getMessage());
            metrics.recordFilterEnableSuccess(); // Expected scenario
            return true;

        } catch (Exception e) {
            log.error("CRITICAL: Failed to enable Hibernate tenant filter", e);
            metrics.recordFilterEnableFailure();
            return false;
        }
    }

    /**
     * Clears tenant context for this thread.
     *
     * CRITICAL: Must always run to prevent thread pool pollution.
     */
    private void clearTenantContext() {
        if (TenantContext.hasTenantContext()) {
            log.trace("Clearing tenant context");
            TenantContext.clear();
        }
    }

    /**
     * Gets authenticated username for logging.
     *
     * @return username if available, "anonymous" otherwise
     */
    private String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User) {
            return ((User) principal).getUsername();
        }

        if (principal instanceof String) {
            return (String) principal;
        }

        return authentication.getName() != null ? authentication.getName() : "unknown";
    }

    /**
     * Simple path-based whitelist for public endpoints.
     *
     * NOTE: @PublicEndpoint annotation check is in TenantInterceptor
     * (after handler resolution, more reliable).
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();

        boolean shouldSkip = path.startsWith("/api/auth/register") ||
                path.startsWith("/api/auth/login") ||
                path.startsWith("/api/public") ||
                path.startsWith("/actuator") ||
                path.startsWith("/error") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs");

        if (shouldSkip) {
            metrics.recordFilterSkip();
            log.trace("Skipping tenant filter for public path: {}", path);
        }

        return shouldSkip;
    }

    private Long extractTenantIdFromJwt(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authHeader.substring(7);

        try {
            return jwtUtil.extractTenantId(token);  // ✅ Method renamed
        } catch (Exception e) {
            log.warn("Failed to extract tenantId from JWT: {}", e.getMessage());
            return null;
        }
    }
}