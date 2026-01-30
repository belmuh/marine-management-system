package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.domain.entity.MainCategory;
import com.marine.management.modules.finance.domain.entity.TenantMainCategory;
import com.marine.management.modules.finance.domain.entity.TenantWhoSelection;
import com.marine.management.modules.finance.domain.entity.Who;
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
        //Get tenantId from context (will throw if not set)
        Long tenantId = TenantContext.getCurrentTenantId();

        logger.info("Initializing reference data for tenant: {}", tenantId);

        initializeMainCategories();
        initializeWhoSelections();

        logger.info("Reference data initialization completed for tenant: {}", tenantId);
    }

    /**
     * Copy all global MainCategory entries to tenant-specific records.
     * All categories start as INACTIVE - user selects during onboarding.
     */
    private void initializeMainCategories() {
        List<MainCategory> globalCategories = mainCategoryRepository.findAll();

        logger.info("Creating {} TenantMainCategory records for tenant: {}",
                globalCategories.size(), TenantContext.getCurrentTenantId());

        for (MainCategory globalCategory : globalCategories) {
            //TenantEntityListener otomatik tenant_id ekler
            TenantMainCategory tenantCategory = TenantMainCategory.create(globalCategory);
            tenantMainCategoryRepository.save(tenantCategory);
        }

        logger.info(" MainCategory initialization completed: {} records", globalCategories.size());
    }

    /**
     * Copy all global WHO entries to tenant-specific records.
     * All WHO selections start as INACTIVE - user selects during onboarding.
     */
    private void initializeWhoSelections() {
        List<Who> globalWhoList = whoRepository.findAll();

        logger.info("Creating {} TenantWhoSelection records for tenant: {}",
                globalWhoList.size(), TenantContext.getCurrentTenantId());

        for (Who globalWho : globalWhoList) {
            //TenantEntityListener otomatik tenant_id ekler
            TenantWhoSelection tenantWho = TenantWhoSelection.create(globalWho);
            tenantWhoSelectionRepository.save(tenantWho);
        }

        logger.info(" WHO selection initialization completed: {} records", globalWhoList.size());
    }
}