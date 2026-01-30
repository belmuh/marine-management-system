package com.marine.management.shared.multitenant;

import com.marine.management.modules.users.domain.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Tenant Context'i kur
        boolean tenantEstablished = establishTenantContext();

        try {
            // 2. İsteği devam ettir (Filtre açma işini Aspect yapacak)
            filterChain.doFilter(request, response);
        } finally {
            // 3. Temizlik
            if (tenantEstablished) {
                clearTenantContext();
            }
        }
    }

    private boolean establishTenantContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof User user) {

            if (user.getOrganization() != null) {
                Long tenantId = user.getOrganization().getOrganizationId();
                TenantContext.setCurrentTenantId(tenantId);
                log.trace("Tenant context set: {}", tenantId);
                return true;
            }
        }
        return false;
    }

    private void clearTenantContext() {
        TenantContext.clear();
        log.trace("Tenant context cleared");
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/public") ||
                path.startsWith("/api/auth") ||
                path.startsWith("/actuator");
    }
}