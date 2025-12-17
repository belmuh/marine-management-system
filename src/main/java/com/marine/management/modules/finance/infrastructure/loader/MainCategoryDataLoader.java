package com.marine.management.modules.finance.infrastructure.loader;

import com.marine.management.modules.finance.domain.entity.MainCategory;
import com.marine.management.modules.finance.infrastructure.MainCategoryRepository;
import com.marine.management.shared.config.GlobalExceptionHandler;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;

@Component
@Order(1)
public class MainCategoryDataLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
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

        logger.info("Loading main category seed data...");

        List<MainCategory> categories = Arrays.asList(
                createCategory("CREW_EXPENSES", "Personel Giderleri", "Crew Expenses", false),
                createCategory("MAINTENANCE", "Bakım ve Onarım", "Maintenance & Repair", true),
                createCategory("GUEST", "Misafir Giderleri", "Guest Expenses", false),
                createCategory("OWNER", "Tekne Sahibi Giderleri", "Owner Expenses", false),
                createCategory("OPERATIONAL", "Operasyonel Giderler", "Operational Expenses", false),
                createCategory("FUEL", "Yakıt", "Fuel", true),
                createCategory("PROVISIONS", "Kumanya / Erzak", "Provisions", false)
        );

        mainCategoryRepository.saveAll(categories);
        // MAIN CATEGORY - WHO LINK
        categories.forEach(category -> {
            DataLoaderSharedData.addMainCategory(category.getCode(), category.getId());
        });
        logger.info("Main category seed data loaded successfully: {} categories", categories.size());
    }

    private MainCategory createCategory(String code, String nameTr, String nameEn, boolean isTechnical) {
        MainCategory category = new MainCategory();
        // ID'yi set etme! Database auto-generate yapacak
        category.setCode(code);
        category.setNameTr(nameTr);
        category.setNameEn(nameEn);
        category.setTechnical(isTechnical);
        category.setActive(true);
        return category;
    }
}
