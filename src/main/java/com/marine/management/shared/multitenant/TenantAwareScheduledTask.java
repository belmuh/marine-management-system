package com.marine.management.shared.multitenant;

import com.marine.management.modules.organization.domain.Organization;
import com.marine.management.modules.organization.infrastructure.OrganizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Base class for scheduled tasks that need to run for all tenants.
 *
 * Automatically handles:
 * - Loading all active tenants
 * - Setting TenantContext for each tenant
 * - Clearing TenantContext after each tenant
 * - Error isolation (one tenant's failure doesn't stop others)
 *
 * Usage:
 * <pre>
 * @Component
 * public class DailyReportTask extends TenantAwareScheduledTask {
 *
 *     @Scheduled(cron = "0 0 2 * * *")
 *     public void generateDailyReports() {
 *         executeForAllTenants(tenant -> {
 *             // Your business logic here
 *             // TenantContext is already set!
 *             List<FinancialEntry> entries = entryRepository.findAll();
 *             // ...
 *         });
 *     }
 * }
 * </pre>
 */
public abstract class TenantAwareScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(TenantAwareScheduledTask.class);

    @Autowired
    private OrganizationRepository organizationRepository;

    /**
     * Executes the given task for all active tenants.
     *
     * @param task business logic to execute (receives tenant as parameter)
     */
    protected void executeForAllTenants(TenantTask task) {
        List<Organization> tenants = organizationRepository.findAllByActiveTrue();

        log.info("Executing task for {} active tenants", tenants.size());

        int successCount = 0;
        int failureCount = 0;

        for (Organization tenant : tenants) {
            try {
                log.debug("Executing task for tenant: {} (id={})",
                        tenant.getYachtName(),
                        tenant.getId());

                TenantContext.setCurrentTenantId(tenant.getId());

                task.execute(tenant);

                successCount++;

            } catch (Exception e) {
                failureCount++;
                log.error("Task failed for tenant: {} (id={})",
                        tenant.getYachtName(),
                        tenant.getId(),
                        e);
                // Continue with next tenant

            } finally {
                TenantContext.clear();
            }
        }

        log.info("Task execution completed: {} succeeded, {} failed",
                successCount,
                failureCount);
    }

    @FunctionalInterface
    protected interface TenantTask {
        void execute(Organization tenant) throws Exception;
    }
}