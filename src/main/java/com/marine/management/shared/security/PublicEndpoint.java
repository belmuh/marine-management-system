package com.marine.management.shared.security;

import java.lang.annotation.*;

/**
 * Marks a controller method or class as a public endpoint.
 *
 * Public endpoints:
 * - Skip tenant context validation
 * - Do not require authentication
 * - Accessible without organization membership
 *
 * USAGE:
 *
 * Class-level (all methods are public):
 * <pre>
 * @RestController
 * @RequestMapping("/api/auth")
 * @PublicEndpoint
 * public class AuthController {
 *     // All methods are public
 * }
 * </pre>
 *
 * Method-level (specific method is public):
 * <pre>
 * @RestController
 * @RequestMapping("/api/finance")
 * public class FinanceController {
 *
 *     @GetMapping("/health")
 *     @PublicEndpoint
 *     public String health() {
 *         return "OK";
 *     }
 *
 *     @GetMapping("/entries")
 *     public List<Entry> entries() {
 *         // Protected - requires tenant context
 *     }
 * }
 * </pre>
 *
 * SECURITY NOTE:
 * Be very careful when using this annotation.
 * Only use for truly public endpoints (auth, health checks, public API).
 *
 * @see com.marine.management.shared.multitenant.TenantInterceptor
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PublicEndpoint {

    /**
     * Optional description of why this endpoint is public.
     * Useful for documentation and security audits.
     */
    String reason() default "";
}