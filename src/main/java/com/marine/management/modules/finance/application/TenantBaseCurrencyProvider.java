package com.marine.management.modules.finance.application;

import com.marine.management.modules.organization.infrastructure.OrganizationRepository;
import com.marine.management.shared.multitenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Provides the base currency for the current tenant.
 *
 * Reads the tenant's selected base currency from the Organization entity,
 * falling back to EUR only if the organization is not found.
 */
@Component
public class TenantBaseCurrencyProvider {

    private static final Logger log = LoggerFactory.getLogger(TenantBaseCurrencyProvider.class);
    private static final String FALLBACK_CURRENCY = "EUR";

    private final OrganizationRepository organizationRepository;

    public TenantBaseCurrencyProvider(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    /**
     * Returns the base currency for the current tenant.
     *
     * @return ISO 4217 currency code (e.g. "EUR", "USD", "GBP")
     */
    public String getCurrentTenantBaseCurrency() {
        Long tenantId = TenantContext.getCurrentTenantId();

        return organizationRepository.findById(tenantId)
                .map(org -> {
                    String currency = org.getBaseCurrency();
                    if (currency == null || currency.isBlank()) {
                        log.warn("Organization {} has no base currency set, falling back to {}", tenantId, FALLBACK_CURRENCY);
                        return FALLBACK_CURRENCY;
                    }
                    return currency;
                })
                .orElseGet(() -> {
                    log.warn("Organization not found for tenantId={}, falling back to {}", tenantId, FALLBACK_CURRENCY);
                    return FALLBACK_CURRENCY;
                });
    }
}
