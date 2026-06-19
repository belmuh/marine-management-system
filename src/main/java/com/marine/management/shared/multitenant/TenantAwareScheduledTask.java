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
 *     @Autowired
 *     private ReportService reportService;  // @Service anotasyonlu olmalı
 *
 *     @Scheduled(cron = "0 0 2 * * *")
 *     public void generateDailyReports() {
 *         executeForAllTenants(tenant -> {
 *             // Lambda içinde MUTLAKA @Service metoduna delege et.
 *             reportService.generateDaily();   // ✅ doğru — AOP proxy devreye girer
 *
 *             // ❌ YANLIŞ — repository'yi doğrudan ÇAĞIRMA:
 *             // entryRepository.findAll();
 *             // JpaRepository interface metodları @annotation(Transactional) AOP
 *             // pointcut'ına uymaz → TenantFilterAspect tetiklenmez → Hibernate @Filter
 *             // aktif olmaz → TenantContext set edilmiş olsa bile tüm tenant'ların
 *             // verisi döner (cross-tenant sızıntı).
 *         });
 *     }
 * }
 * </pre>
 *
 * <p><strong>⚠️ Mimari Not (inceleme bekliyor):</strong>
 * TenantFilterAspect, Hibernate filtreni yalnızca Spring AOP proxy zinciri üzerinden
 * aktif eder (@Service, @Transactional). Background job context'inde proxy zinciri dışında
 * kalan doğrudan çağrılar filtreyi atlayabilir. Alternatif: executeForAllTenants içinde
 * session.enableFilter(...) explicit çağrısı eklenebilir — AOP'tan bağımsız, daha güvenli.
 * Bkz. TODO.md → "TenantAwareScheduledTask — AOP dışı filter aktivasyonu" maddesi.
 * </p>
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

                TenantContext.setCurrentTenantId(tenant.getOrganizationId());

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