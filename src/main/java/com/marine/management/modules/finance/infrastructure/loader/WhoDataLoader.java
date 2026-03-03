package com.marine.management.modules.finance.infrastructure.loader;

import com.marine.management.modules.finance.domain.entities.Who;
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
        Long fuelId = DataLoaderSharedData.getMainCategoryId("FUEL");


        List<Who> whoList = Arrays.asList(
                // PERSONAL (6)
                createWho("CAPTAIN", "Kaptan", "Captain", false, 1, crewExpensesId),
                createWho("CREW", "Personel", "Crew", false, 2, crewExpensesId),
                createWho("GUEST", "Misafir", "Guest", false, 3, provisionsId),
                createWho("OWNER", "Tekne Sahibi", "Owner", false, 4, provisionsId),
                createWho("OFFICE", "Ofis", "Office", false, 5, administrationId),
                createWho("MARINA", "Marina", "Marina", false, 6, dockageId),

                // TECHNICAL (10) - For both Maintenance and Fuel
                createWho("MAIN_ENGINE", "Ana Makine", "Main Engine", true, 7, maintenanceId),
                createWho("GENERATOR", "Jeneratör", "Generator", true, 8,maintenanceId),
                createWho("TENDER", "Tender", "Tender", true, 9, fuelId),
                createWho("JETSKI", "Jetski", "Jetski", true, 10, fuelId),
                createWho("AC_SYSTEM", "Klima", "AC System", true, 11, maintenanceId),
                createWho("WATERMAKER", "Su Yapıcı", "Watermaker", true, 12, maintenanceId),
                createWho("ELECTRICAL", "Elektrik", "Electrical", true, 13, maintenanceId),
                createWho("PLUMBING", "Tesisat", "Plumbing", true, 14, maintenanceId),
                createWho("ELECTRONICS", "Elektronik", "Electronics", true, 15, maintenanceId),
                createWho("HULL", "Tekne Gövdesi", "Hull", true, 16, maintenanceId)
        );

        whoRepository.saveAll(whoList);

        long technicalCount = whoList.stream().filter(Who::isTechnical).count();
        long personalCount = whoList.stream().filter(w -> !w.isTechnical()).count();

        logger.info(" ISS standard who list loaded successfully: {} entries", whoList.size());
        logger.info("   - Technical WHO: {}", technicalCount);
        logger.info("   - Personal WHO: {}", personalCount);
    }

    /**
     * Creates a Who entry using factory method.
     *
     *  FIXED: Uses Who.create() instead of new Who()
     */
    private Who createWho(
            String code,
            String nameTr,
            String nameEn,
            boolean isTechnical,
            int displayOrder,
            Long suggestedMainCategoryId
    ) {
        return Who.create(
                code,
                nameTr,
                nameEn,
                isTechnical,
                displayOrder,
                suggestedMainCategoryId
        );
    }
}