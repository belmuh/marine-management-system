package com.marine.management.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.marine.management.shared.presentation.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IP-based rate limiting filter using Caffeine fixed-window counters.
 *
 * Runs BEFORE Spring Security — requests are rejected at the servlet level
 * before any authentication or tenant processing occurs.
 *
 * Why Caffeine (not Redis):
 * - No additional infrastructure dependency for MVP
 * - In-memory is sufficient for single-instance deployment
 * - Caffeine is already in the project for reference data caching
 * - Migrate to Redis-backed Bucket4j when horizontal scaling is needed
 *
 * Rate limit rules:
 * - Login / forgot-password / reset-password: 5 attempts per minute per IP
 * - Registration / resend-verification:       3 attempts per hour per IP
 *
 * Headers returned on 429:
 * - Retry-After:      seconds until the current window resets
 * - X-RateLimit-Reset: unix timestamp of the reset moment
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)  // Run before Spring Security in the servlet filter chain
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    // --- Auth endpoints: 5 requests per 60 seconds per IP ---
    private static final int  AUTH_LIMIT          = 5;
    private static final long AUTH_WINDOW_SECONDS = 60;

    // --- Registration: 3 requests per hour per IP ---
    private static final int  REGISTER_LIMIT          = 3;
    private static final long REGISTER_WINDOW_SECONDS = 3_600;

    /**
     * Separate caches with matching TTLs so the counter resets exactly
     * when the window expires — no manual timestamp bookkeeping needed.
     */
    private final Cache<String, AtomicInteger> authCache = Caffeine.newBuilder()
            .expireAfterWrite(AUTH_WINDOW_SECONDS, TimeUnit.SECONDS)
            .maximumSize(10_000)   // ~10k concurrent IPs in the auth window
            .build();

    private final Cache<String, AtomicInteger> registerCache = Caffeine.newBuilder()
            .expireAfterWrite(REGISTER_WINDOW_SECONDS, TimeUnit.SECONDS)
            .maximumSize(5_000)
            .build();

    private final ObjectMapper objectMapper;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // -------------------------------------------------------------------------
    // Filter logic
    // -------------------------------------------------------------------------

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String path   = request.getRequestURI();
        String method = request.getMethod();

        // Only POST requests to sensitive public endpoints are rate-limited.
        // Authenticated endpoints are protected differently (JWT + roles).
        RateLimitRule rule = resolveRule(method, path);

        if (rule != null) {
            String clientIp = resolveClientIp(request);

            if (isLimitExceeded(clientIp, path, rule)) {
                log.warn("Rate limit exceeded | ip={} path={} method={}", clientIp, path, method);
                sendRateLimitResponse(response, rule.windowSeconds());
                return;
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Skip rate limiting for non-API paths (static assets, actuator).
     * Avoids unnecessary cache lookups for every request.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/auth/")
                && !path.startsWith("/api/onboarding/");
    }

    // -------------------------------------------------------------------------
    // Rule resolution
    // -------------------------------------------------------------------------

    /**
     * Maps (method, path) → the applicable rate limit rule.
     * Returns null when no limit should be applied (e.g. GET requests).
     */
    private RateLimitRule resolveRule(String method, String path) {
        if (!"POST".equalsIgnoreCase(method)) {
            return null;
        }

        return switch (path) {
            case "/api/auth/login",
                 "/api/auth/forgot-password",
                 "/api/auth/reset-password"
                    -> new RateLimitRule(authCache, AUTH_LIMIT, AUTH_WINDOW_SECONDS);

            case "/api/auth/register",
                 "/api/auth/resend-verification"
                    -> new RateLimitRule(registerCache, REGISTER_LIMIT, REGISTER_WINDOW_SECONDS);

            default -> null;
        };
    }

    // -------------------------------------------------------------------------
    // Counter logic
    // -------------------------------------------------------------------------

    /**
     * Increments the counter for this (IP, path) pair and checks the limit.
     *
     * Caffeine's expireAfterWrite TTL automatically resets the counter when
     * the window expires — no manual timestamp management required.
     */
    private boolean isLimitExceeded(String clientIp, String path, RateLimitRule rule) {
        String cacheKey = clientIp + ":" + path;
        AtomicInteger counter = rule.cache().get(cacheKey, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();
        return count > rule.limit();
    }

    // -------------------------------------------------------------------------
    // IP resolution
    // -------------------------------------------------------------------------

    /**
     * Resolves the real client IP, handling reverse proxies and load balancers.
     *
     * Priority: X-Forwarded-For (leftmost = original client) → X-Real-IP → RemoteAddr
     *
     * NOTE: X-Forwarded-For can be spoofed if the reverse proxy does not strip
     * existing headers. Ensure Nginx / Caddy is configured to overwrite this
     * header rather than append to it.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // "client, proxy1, proxy2" — take leftmost (original client)
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }

    // -------------------------------------------------------------------------
    // 429 response
    // -------------------------------------------------------------------------

    private void sendRateLimitResponse(
            HttpServletResponse response,
            long retryAfterSeconds
    ) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Standard headers so clients can back off gracefully
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setHeader(
                "X-RateLimit-Reset",
                String.valueOf(System.currentTimeMillis() / 1000 + retryAfterSeconds)
        );

        ErrorResponse body = new ErrorResponse(
                "Too many requests",
                "Rate limit exceeded. Please wait and try again.",
                "RATE_LIMIT_EXCEEDED",
                UUID.randomUUID().toString()
        );

        objectMapper.writeValue(response.getWriter(), body);
    }

    // -------------------------------------------------------------------------
    // Value type
    // -------------------------------------------------------------------------

    /**
     * Immutable rule binding a cache, its limit, and window duration.
     */
    private record RateLimitRule(
            Cache<String, AtomicInteger> cache,
            int limit,
            long windowSeconds
    ) {}
}
