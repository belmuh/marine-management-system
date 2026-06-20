package com.marine.management.shared.multitenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Hibernate integration for multi-tenant support.
 *
 * This resolver provides the current tenant identifier to Hibernate's
 * multi-tenancy mechanism. When configured with DISCRIMINATOR strategy,
 * Hibernate will automatically add WHERE tenant_id = ? to all queries.
 *
 * Integration: Must be registered in application.yml under hibernate properties
 * Fallback: Returns "SYSTEM" tenant for background jobs without tenant context
 *
 * Configuration Required:
 * <pre>
 * spring:
 *   jpa:
 *     properties:
 *       hibernate:
 *         multiTenancy: DISCRIMINATOR
 *         tenant_identifier_resolver: com.marine.management.shared.multitenant.HibernateTenantIdentifierResolver
 * </pre>
 *
 * @see TenantContext
 */
@Component("hibernateTenantIdentifierResolver")
public class HibernateTenantIdentifierResolver implements CurrentTenantIdentifierResolver {

    private static final Logger log = LoggerFactory.getLogger(HibernateTenantIdentifierResolver.class);

    private static final String SYSTEM_TENANT = "SYSTEM";

    @Override
    public String resolveCurrentTenantIdentifier() {
        if (!TenantContext.hasTenantContext()) {
            // SYSTEM fallback kasıtlı bir tasarım kararıdır — bug değil.
            // Spring Security init ve /api/auth/** endpoint'lerinde (login, refresh)
            // henüz tenant set edilmemiş olabilir. Bu durumda Hibernate
            // WHERE tenant_id = 'SYSTEM' uygular; hiçbir satır bu değerle
            // kayıtlı olmadığından read'ler boş döner, cross-tenant sızıntı olmaz.
            // Write koruması TenantEntityListener tarafından sağlanıyor.
            log.trace("No tenant context, using SYSTEM tenant for background operation");
            return SYSTEM_TENANT;
        }

        Long tenantId = TenantContext.getCurrentTenantId();
        log.trace("Resolved tenant identifier: {}", tenantId);

        return tenantId.toString();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        // We want existing sessions to be validated against current tenant
        return true;
    }
}
