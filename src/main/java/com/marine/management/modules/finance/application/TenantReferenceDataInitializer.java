package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.domain.entities.MainCategory;
import com.marine.management.modules.finance.domain.entities.TenantMainCategory;
import com.marine.management.modules.finance.domain.entities.TenantWhoSelection;
import com.marine.management.modules.finance.domain.entities.Who;
import com.marine.management.modules.finance.infrastructure.MainCategoryRepository;
import com.marine.management.modules.finance.infrastructure.TenantMainCategoryRepository;
import com.marine.management.modules.finance.infrastructure.TenantWhoSelectionRepository;
import com.marine.management.modules.finance.infrastructure.WhoRepository;
import com.marine.management.shared.multitenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Initializes tenant-specific reference data on new tenant registration.
 *
 * Creates TenantMainCategory and TenantWhoSelection records
 * from global MainCategory and Who seed data.
 */
@Service
public class TenantReferenceDataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(TenantReferenceDataInitializer.class);

    private final MainCategoryRepository mainCategoryRepository;
    private final WhoRepository whoRepository;
    private final TenantMainCategoryRepository tenantMainCategoryRepository;
    private final TenantWhoSelectionRepository tenantWhoSelectionRepository;

    public TenantReferenceDataInitializer(
            MainCategoryRepository mainCategoryRepository,
            WhoRepository whoRepository,
            TenantMainCategoryRepository tenantMainCategoryRepository,
            TenantWhoSelectionRepository tenantWhoSelectionRepository
    ) {
        this.mainCategoryRepository = mainCategoryRepository;
        this.whoRepository = whoRepository;
        this.tenantMainCategoryRepository = tenantMainCategoryRepository;
        this.tenantWhoSelectionRepository = tenantWhoSelectionRepository;
    }

    /**
     * Initialize all reference data for current tenant.
     * Called during registration.
     *
     * ⚠️ REQUIRES: TenantContext must be set before calling!
     */
    @Transactional
    public void initializeTenantReferenceData() {
        // Get tenantId from context (will throw if not set)
        Long tenantId = TenantContext.getCurrentTenantId();

        logger.info("🔄 Initializing reference data for tenant: {}", tenantId);

        initializeMainCategories();
        initializeWhoSelections();

        logger.info(" Reference data initialization completed for tenant: {}", tenantId);
    }

    /**
     * Copy all global MainCategory entries to tenant-specific records.
     * All categories start as INACTIVE - user selects during onboarding.
     */
    private void initializeMainCategories() {
        List<MainCategory> globalCategories = mainCategoryRepository.findAll();

        logger.info("📦 Found {} global MainCategories for tenant: {}",
                globalCategories.size(), TenantContext.getCurrentTenantId());

        if (globalCategories.isEmpty()) {
            logger.warn("⚠️ No global MainCategories found! Skipping tenant initialization.");
            return;
        }

        //  Create all tenant categories in batch
        List<TenantMainCategory> tenantCategories = globalCategories.stream()
                .map(TenantMainCategory::create)
                .toList();

        //  Save all at once
        tenantMainCategoryRepository.saveAll(tenantCategories);
        tenantMainCategoryRepository.flush(); // Force immediate write

        logger.info(" Created {} TenantMainCategory records for tenant: {}",
                tenantCategories.size(), TenantContext.getCurrentTenantId());
    }

    /**
     * Copy all global WHO entries to tenant-specific records.
     * All WHO selections start as INACTIVE - user selects during onboarding.
     */
    private void initializeWhoSelections() {
        List<Who> globalWhoList = whoRepository.findAll();

        logger.info("📦 Found {} global WHO records for tenant: {}",
                globalWhoList.size(), TenantContext.getCurrentTenantId());

        if (globalWhoList.isEmpty()) {
            logger.warn("⚠️ No global WHO records found! Skipping tenant initialization.");
            return;
        }

        //  Create all tenant WHO selections in batch
        List<TenantWhoSelection> tenantWhoSelections = globalWhoList.stream()
                .map(TenantWhoSelection::create)
                .toList();

        //  Save all at once
        tenantWhoSelectionRepository.saveAll(tenantWhoSelections);
        tenantWhoSelectionRepository.flush(); // Force immediate write

        logger.info(" Created {} TenantWhoSelection records for tenant: {}",
                tenantWhoSelections.size(), TenantContext.getCurrentTenantId());
    }
}