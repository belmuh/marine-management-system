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
import java.util.Set;

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
     * Called during registration. Enables all by default.
     *
     * ⚠️ REQUIRES: TenantContext must be set before calling!
     */
    @Transactional
    public void initializeTenantReferenceData() {
        initializeTenantReferenceData(null, null);
    }

    /**
     * Initialize reference data for current tenant with optional selections.
     *
     * @param selectedMainCategoryIds IDs of main categories to enable (null = enable all)
     * @param selectedWhoIds IDs of WHO entries to enable (null = enable all)
     *
     * ⚠️ REQUIRES: TenantContext must be set before calling!
     */
    @Transactional
    public void initializeTenantReferenceData(Set<Long> selectedMainCategoryIds, Set<Long> selectedWhoIds) {
        Long tenantId = TenantContext.getCurrentTenantId();

        logger.info("Initializing reference data for tenant: {}", tenantId);

        initializeMainCategories(selectedMainCategoryIds);
        initializeWhoSelections(selectedWhoIds);

        logger.info("Reference data initialization completed for tenant: {}", tenantId);
    }

    /**
     * Copy all global MainCategory entries to tenant-specific records.
     * If selectedIds is provided, only those are enabled; others are disabled.
     * If selectedIds is null, all are enabled.
     */
    private void initializeMainCategories(Set<Long> selectedIds) {
        List<MainCategory> globalCategories = mainCategoryRepository.findAll();

        logger.info("Found {} global MainCategories for tenant: {}",
                globalCategories.size(), TenantContext.getCurrentTenantId());

        if (globalCategories.isEmpty()) {
            logger.warn("No global MainCategories found! Skipping tenant initialization.");
            return;
        }

        boolean enableAll = selectedIds == null || selectedIds.isEmpty();

        List<TenantMainCategory> tenantCategories = globalCategories.stream()
                .map(mc -> {
                    TenantMainCategory tmc = TenantMainCategory.create(mc);
                    if (!enableAll && !selectedIds.contains(mc.getId())) {
                        tmc.disable();
                    }
                    return tmc;
                })
                .toList();

        tenantMainCategoryRepository.saveAll(tenantCategories);
        tenantMainCategoryRepository.flush();

        long enabledCount = tenantCategories.stream().filter(TenantMainCategory::isEnabled).count();
        logger.info("Created {} TenantMainCategory records ({} enabled) for tenant: {}",
                tenantCategories.size(), enabledCount, TenantContext.getCurrentTenantId());
    }

    /**
     * Copy all global WHO entries to tenant-specific records.
     * If selectedIds is provided, only those are enabled; others are disabled.
     * If selectedIds is null, all are enabled.
     */
    private void initializeWhoSelections(Set<Long> selectedIds) {
        List<Who> globalWhoList = whoRepository.findAll();

        logger.info("Found {} global WHO records for tenant: {}",
                globalWhoList.size(), TenantContext.getCurrentTenantId());

        if (globalWhoList.isEmpty()) {
            logger.warn("No global WHO records found! Skipping tenant initialization.");
            return;
        }

        boolean enableAll = selectedIds == null || selectedIds.isEmpty();

        List<TenantWhoSelection> tenantWhoSelections = globalWhoList.stream()
                .map(w -> {
                    TenantWhoSelection tws = TenantWhoSelection.create(w);
                    if (!enableAll && !selectedIds.contains(w.getId())) {
                        tws.disable();
                    }
                    return tws;
                })
                .toList();

        tenantWhoSelectionRepository.saveAll(tenantWhoSelections);
        tenantWhoSelectionRepository.flush();

        long enabledCount = tenantWhoSelections.stream().filter(TenantWhoSelection::isEnabled).count();
        logger.info("Created {} TenantWhoSelection records ({} enabled) for tenant: {}",
                tenantWhoSelections.size(), enabledCount, TenantContext.getCurrentTenantId());
    }
}