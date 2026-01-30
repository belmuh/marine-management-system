package com.marine.management.modules.finance.infrastructure.loader;

import com.marine.management.modules.finance.domain.entity.MainCategory;
import com.marine.management.modules.finance.infrastructure.MainCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Loads ISS standard main categories on application startup.
 *
 * GLOBAL DATA:
 * - MainCategory is NOT tenant-isolated (shared reference data)
 * - Loaded once, used by all tenants
 * - TenantMainCategory links tenants to these categories
 */
@Component
@Order(1)  // Load before WHO data
public class MainCategoryDataLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(MainCategoryDataLoader.class);

    private final MainCategoryRepository mainCategoryRepository;

    public MainCategoryDataLoader(MainCategoryRepository mainCategoryRepository) {
        this.mainCategoryRepository = mainCategoryRepository;
    }

    @Override
    public void run(String... args) {
        if (mainCategoryRepository.count() > 0) {
            logger.info("Main categories already exist, skipping seed data");
            return;
        }

        logger.info("Loading ISS standard main category seed data...");

        List<MainCategory> categories = Arrays.asList(
                // ISS Standard Categories with Budget Guidelines
                createCategory("CREW_EXPENSES", "Personel Giderleri", "Crew Expenses",
                        false, 1, "20%", "30%"),
                createCategory("MAINTENANCE_REPAIRS", "Bakım ve Onarım", "Maintenance & Repairs",
                        true, 2, "25%", "35%"),
                createCategory("DOCKAGE_BERTHS", "Liman ve Bağlama", "Dockage & Berths",
                        false, 3, "10%", "15%"),
                createCategory("FUEL", "Yakıt", "Fuel",
                        true, 4, "10%", "20%"),
                createCategory("INSURANCE", "Sigorta", "Insurance",
                        false, 5, "5%", "10%"),
                createCategory("PROVISIONS", "Kumanya / Erzak", "Provisions",
                        false, 6, "5%", "10%"),
                createCategory("ADMINISTRATION", "İdari Giderler", "Administration",
                        false, 7, "3%", "5%")
        );

        mainCategoryRepository.saveAll(categories);

        // Store for WHO linking
        categories.forEach(category -> {
            DataLoaderSharedData.addMainCategory(category.getCode(), category.getId());
        });

        logger.info("✅ ISS standard main categories loaded successfully: {} categories", categories.size());
        logger.info("   - Technical categories: {}",
                categories.stream().filter(MainCategory::isTechnical).count());
        logger.info("   - Personal categories: {}",
                categories.stream().filter(c -> !c.isTechnical()).count());
    }

    /**
     * Creates a MainCategory using factory method.
     *
     */
    private MainCategory createCategory(
            String code,
            String nameTr,
            String nameEn,
            boolean isTechnical,
            int displayOrder,
            String budgetMin,
            String budgetMax
    ) {
        return MainCategory.create(
                code,
                nameTr,
                nameEn,
                isTechnical,
                displayOrder,
                budgetMin,
                budgetMax
        );
    }
}