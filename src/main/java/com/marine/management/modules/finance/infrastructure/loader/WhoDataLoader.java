package com.marine.management.modules.finance.infrastructure.loader;

import com.marine.management.modules.finance.domain.entity.Who;
import com.marine.management.modules.finance.infrastructure.WhoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Loads ISS standard WHO list on application startup.
 *
 * GLOBAL DATA:
 * - Who is NOT tenant-isolated (shared reference data)
 * - Loaded once, used by all tenants
 * - TenantWhoSelection links tenants to these WHO entries
 */
@Component
@Order(2)  // After MainCategoryDataLoader
public class WhoDataLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(WhoDataLoader.class);

    private final WhoRepository whoRepository;

    public WhoDataLoader(WhoRepository whoRepository) {
        this.whoRepository = whoRepository;
    }

    @Override
    public void run(String... args) {
        if (whoRepository.count() > 0) {
            logger.info("Who list already exists, skipping seed data");
            return;
        }

        logger.info("Loading ISS standard who list seed data...");

        // Get main category IDs from shared data
        Long crewExpensesId = DataLoaderSharedData.getMainCategoryId("CREW_EXPENSES");
        Long maintenanceId = DataLoaderSharedData.getMainCategoryId("MAINTENANCE_REPAIRS");
        Long dockageId = DataLoaderSharedData.getMainCategoryId("DOCKAGE_BERTHS");
        Long provisionsId = DataLoaderSharedData.getMainCategoryId("PROVISIONS");
        Long administrationId = DataLoaderSharedData.getMainCategoryId("ADMINISTRATION");

        List<Who> whoList = Arrays.asList(
                // PERSONAL (6)
                createWho("CAPTAIN", "Kaptan", "Captain", false, crewExpensesId),
                createWho("CREW", "Personel", "Crew", false, crewExpensesId),
                createWho("GUEST", "Misafir", "Guest", false, provisionsId),
                createWho("OWNER", "Tekne Sahibi", "Owner", false, provisionsId),
                createWho("OFFICE", "Ofis", "Office", false, administrationId),
                createWho("MARINA", "Marina", "Marina", false, dockageId),

                // TECHNICAL (10) - For both Maintenance and Fuel
                createWho("MAIN_ENGINE", "Ana Makine", "Main Engine", true, null),
                createWho("GENERATOR", "Jeneratör", "Generator", true, null),
                createWho("TENDER", "Tender", "Tender", true, null),
                createWho("JETSKI", "Jetski", "Jetski", true, maintenanceId),
                createWho("AC_SYSTEM", "Klima", "AC System", true, maintenanceId),
                createWho("WATERMAKER", "Su Yapıcı", "Watermaker", true, maintenanceId),
                createWho("ELECTRICAL", "Elektrik", "Electrical", true, maintenanceId),
                createWho("PLUMBING", "Tesisat", "Plumbing", true, maintenanceId),
                createWho("ELECTRONICS", "Elektronik", "Electronics", true, maintenanceId),
                createWho("HULL", "Tekne Gövdesi", "Hull", true, maintenanceId)
        );

        whoRepository.saveAll(whoList);

        long technicalCount = whoList.stream().filter(Who::getTechnical).count();
        long personalCount = whoList.stream().filter(w -> !w.getTechnical()).count();

        logger.info("✅ ISS standard who list loaded successfully: {} entries", whoList.size());
        logger.info("   - Technical WHO: {}", technicalCount);
        logger.info("   - Personal WHO: {}", personalCount);
    }

    /**
     * Creates a Who entry using factory method.
     *
     * ✅ FIXED: Uses Who.create() instead of new Who()
     */
    private Who createWho(
            String code,
            String nameTr,
            String nameEn,
            boolean isTechnical,
            Long suggestedMainCategoryId
    ) {
        return Who.create(
                code,
                nameTr,
                nameEn,
                isTechnical,
                suggestedMainCategoryId
        );
    }
}