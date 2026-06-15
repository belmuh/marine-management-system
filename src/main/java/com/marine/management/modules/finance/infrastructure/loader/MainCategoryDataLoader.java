package com.marine.management.modules.finance.infrastructure.loader;

import com.marine.management.modules.finance.domain.entities.MainCategory;
import com.marine.management.modules.finance.infrastructure.MainCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads ISS standard main categories on application startup.
 *
 * GLOBAL DATA:
 * - MainCategory is NOT tenant-isolated (shared reference data)
 * - Loaded once, used by all tenants
 * - TenantMainCategory links tenants to these categories
 *
 * IDEMPOTENT: missing categories (matched by nameEn) are inserted on startup,
 * existing ones are left untouched. This allows adding new main categories
 * (e.g. Communication) to existing databases without a manual migration.
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
        List<MainCategory> definitions = Arrays.asList(
                // ISS Standard Categories with Budget Guidelines
                createCategory("Personel Giderleri", "Crew Expenses",
                        false, 1, "20%", "30%"),
                createCategory("Bakım ve Onarım", "Maintenance & Repairs",
                        true, 2, "25%", "35%"),
                createCategory("Liman ve Bağlama", "Dockage & Berths",
                        false, 3, "10%", "15%"),
                createCategory("Yakıt", "Fuel",
                        true, 4, "10%", "20%"),
                createCategory("Sigorta", "Insurance",
                        false, 5, "5%", "10%"),
                createCategory("Kumanya / Erzak", "Provisions",
                        false, 6, "5%", "10%"),
                createCategory("İdari Giderler", "Administration",
                        false, 7, "3%", "5%"),
                // Piyasa analizi sonucu eklendi (2026-06): Starlink/VSAT, telefon vb.
                createCategory("İletişim", "Communication",
                        false, 8, "2%", "5%")
        );

        List<MainCategory> existing = mainCategoryRepository.findAll();
        Set<String> existingNames = existing.stream()
                .map(MainCategory::getNameEn)
                .collect(Collectors.toSet());

        List<MainCategory> toInsert = definitions.stream()
                .filter(d -> !existingNames.contains(d.getNameEn()))
                .toList();

        if (toInsert.isEmpty()) {
            logger.info("Main categories up to date ({} existing), no seed needed", existing.size());
        } else {
            mainCategoryRepository.saveAll(toInsert);
            logger.info("Inserted {} missing main categories: {}",
                    toInsert.size(),
                    toInsert.stream().map(MainCategory::getNameEn).toList());
        }

        // Store for WHO linking (using English name as key) — existing + new
        existing.forEach(c -> DataLoaderSharedData.addMainCategory(c.getNameEn(), c.getId()));
        toInsert.forEach(c -> DataLoaderSharedData.addMainCategory(c.getNameEn(), c.getId()));

        logger.info("Main category reference data ready: {} total", existing.size() + toInsert.size());
    }

    /**
     * Creates a MainCategory using factory method.
     */
    private MainCategory createCategory(
            String nameTr,
            String nameEn,
            boolean isTechnical,
            int displayOrder,
            String budgetMin,
            String budgetMax
    ) {
        return MainCategory.create(
                nameTr,
                nameEn,
                isTechnical,
                displayOrder,
                budgetMin,
                budgetMax
        );
    }
}
