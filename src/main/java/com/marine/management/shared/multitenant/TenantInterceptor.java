package com.marine.management.shared.multitenant;

import com.marine.management.shared.security.PublicEndpoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor for tenant context validation and @PublicEndpoint handling.
 *
 * RESPONSIBILITIES:
 * 1. Check @PublicEndpoint annotation (method/class level)
 * 2. Skip tenant validation for public endpoints
 * 3. Validate tenant context for protected endpoints
 *
 * WHY INTERCEPTOR (not Filter)?
 * - Runs AFTER handler resolution (safe)
 * - Has access to HandlerMethod (controller method)
 * - Can check annotations reliably
 * - Works in all Spring MVC flows
 *
 * EXECUTION ORDER:
 * 1. TenantFilter (sets TenantContext, enables Hibernate filter)
 * 2. TenantInterceptor (validates @PublicEndpoint, checks context)
 * 3. Controller method
 *
 * @see TenantFilter
 * @see PublicEndpoint
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TenantInterceptor.class);

    private final TenantFilterMetrics metrics;

    public TenantInterceptor(TenantFilterMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        if (isPublicEndpoint(handlerMethod)) {
            log.trace("@PublicEndpoint detected: {}", handlerMethod.getMethod().getName());
            return true;
        }

        // Protected endpoint - MUST have tenant context
        if (!TenantContext.hasTenantContext()) {
            metrics.recordContextMissing();  // ← CRITICAL METRIC!

            log.error("SECURITY VIOLATION: Protected endpoint without tenant context! " +
                            "Controller: {}, Method: {}, Path: {}",
                    handlerMethod.getBeanType().getSimpleName(),
                    handlerMethod.getMethod().getName(),
                    request.getRequestURI());

            throw new AccessDeniedException("Tenant context required");
        }

        return true;
    }

    /**
     * Checks if handler has @PublicEndpoint annotation.
     *
     * Checks both:
     * - Method level: @GetMapping @PublicEndpoint
     * - Class level: @RestController @PublicEndpoint
     */
    private boolean isPublicEndpoint(HandlerMethod handlerMethod) {
        // Method-level check
        if (handlerMethod.getMethodAnnotation(PublicEndpoint.class) != null) {
            return true;
        }

        // Class-level check
        return handlerMethod.getBeanType().isAnnotationPresent(PublicEndpoint.class);
    }
}