// shared/config/JwtAuthenticationFilter.java
package com.marine.management.shared.config;

import com.marine.management.modules.auth.infrastructure.JwtUtil;
import com.marine.management.modules.users.domain.User;
import com.marine.management.modules.users.infrastructure.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter.
 *
 * CRITICAL: This filter runs BEFORE TenantFilter.
 * At this point, tenant context is NOT yet established.
 * Therefore, userRepository.findByEmail() must be GLOBAL (no tenant filter).
 *
 * JWT contains:
 * - subject: user email (from UserDetails.getUsername())
 * - tenantId: organization ID (custom claim)
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        logger.debug("Filter 'jwtAuthenticationFilter' configured for use");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = authHeader.substring(7);

            // ⭐ Extract email from JWT subject (UserDetails.getUsername())
            String email = jwtUtil.extractUsername(jwt);

            // ⭐ Extract tenantId from JWT claims
            Long tenantId = jwtUtil.extractClaim(jwt, claims -> claims.get("tenantId", Long.class));

            if (email != null && tenantId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // ⭐ GLOBAL EMAIL LOOKUP: No tenant filter at this point
                User user = userRepository.findByEmail(email).orElse(null);

                // ⭐ SECURITY: Verify user belongs to the tenantId from JWT
                if (user != null && user.getOrganizationId().equals(tenantId) && jwtUtil.isTokenValid(jwt, user)) {

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    user.getAuthorities()
                            );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    logger.trace("JWT authentication successful for user: {} in org: {}", email, tenantId);
                } else if (user == null) {
                    logger.warn("User not found with email: {}", email);
                } else if (!user.getOrganizationId().equals(tenantId)) {
                    logger.warn("Tenant mismatch: JWT tenantId={}, User tenantId={}", tenantId, user.getOrganizationId());
                }
            }

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            logger.warn("JWT token expired: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Token expired", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.warn("Malformed JWT token: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid token format", e.getMessage());
        } catch (SignatureException e) {
            logger.warn("Invalid JWT signature: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid token signature", e.getMessage());
        } catch (Exception e) {
            logger.error("JWT authentication error: {}", e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                    "Authentication failed", e.getMessage());
        }
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String error, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(
                String.format("{\"error\":\"%s\",\"message\":\"%s\"}", error, message)
        );
    }
}