package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.domain.entity.TenantMainCategory;
import com.marine.management.modules.finance.domain.entity.TenantWhoSelection;
import com.marine.management.modules.finance.infrastructure.TenantMainCategoryRepository;
import com.marine.management.modules.finance.infrastructure.TenantWhoSelectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Reference data service for tenant-specific selections.
 *
 * DESIGN:
 * - Returns only active WHO and MainCategory choices for current tenant
 * - Filtered automatically by TenantFilter (tenant_id)
 * - Used by UI dropdowns and search filters
 */
@Service
@Transactional(readOnly = true)
public class ReferenceDataService {

    private final TenantMainCategoryRepository tenantMainCategoryRepository;
    private final TenantWhoSelectionRepository tenantWhoSelectionRepository;

    public ReferenceDataService(
            TenantMainCategoryRepository tenantMainCategoryRepository,
            TenantWhoSelectionRepository tenantWhoSelectionRepository
    ) {
        this.tenantMainCategoryRepository = tenantMainCategoryRepository;
        this.tenantWhoSelectionRepository = tenantWhoSelectionRepository;
    }

    // ============================================
    // MAIN CATEGORY QUERIES (Tenant-Filtered)
    // ============================================

    /**
     * Get all active main categories for current tenant.
     * Automatically filtered by tenant_id via Hibernate filter.
     */
    public List<TenantMainCategory> getActiveMainCategories() {
        return tenantMainCategoryRepository.findByIsActiveTrue();
    }

    /**
     * Get active main categories by technical flag.
     * @param technical true for technical (e.g. FUEL, MAINTENANCE), false for personal (e.g. CREW)
     */
    public List<TenantMainCategory> getActiveMainCategoriesByType(boolean technical) {
        return tenantMainCategoryRepository.findByIsActiveTrueAndMainCategory_Technical(technical);
    }

    /**
     * Get tenant main category by ID.
     */
    public Optional<TenantMainCategory> getTenantMainCategoryById(Long id) {
        return tenantMainCategoryRepository.findById(id);
    }

    /**
     * Get tenant main category by global main category code.
     * Useful for onboarding/initialization.
     */
    public Optional<TenantMainCategory> getTenantMainCategoryByCode(String code) {
        return tenantMainCategoryRepository.findByMainCategory_Code(code);
    }

    // ============================================
    // WHO QUERIES (Tenant-Filtered)
    // ============================================

    /**
     * Get all active WHO selections for current tenant.
     * Automatically filtered by tenant_id via Hibernate filter.
     */
    public List<TenantWhoSelection> getActiveWhoSelections() {
        return tenantWhoSelectionRepository.findByIsActiveTrue();
    }

    /**
     * Get active WHO selections by technical flag.
     * @param technical true for equipment (e.g. MAIN_ENGINE), false for people (e.g. CAPTAIN)
     */
    public List<TenantWhoSelection> getActiveWhoSelectionsByType(boolean technical) {
        return tenantWhoSelectionRepository.findByIsActiveTrueAndWho_Technical(technical);
    }

    /**
     * Get WHO selections suggested for a specific main category.
     * Example: FUEL → [MAIN_ENGINE, GENERATOR, TENDER]
     */
    public List<TenantWhoSelection> getWhoSelectionsBySuggestedMainCategory(Long mainCategoryId) {
        return tenantWhoSelectionRepository.findByIsActiveTrueAndWho_SuggestedMainCategoryId(mainCategoryId);
    }

    /**
     * Get tenant WHO selection by ID.
     */
    public Optional<TenantWhoSelection> getTenantWhoSelectionById(Long id) {
        return tenantWhoSelectionRepository.findById(id);
    }

    /**
     * Get tenant WHO selection by global WHO code.
     * Useful for onboarding/initialization.
     */
    public Optional<TenantWhoSelection> getTenantWhoSelectionByCode(String code) {
        return tenantWhoSelectionRepository.findByWho_Code(code);
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Check if tenant has enabled a specific main category.
     */
    public boolean isMainCategoryEnabled(String code) {
        return tenantMainCategoryRepository
                .findByMainCategory_Code(code)
                .map(TenantMainCategory::getActive)
                .orElse(false);
    }

    /**
     * Check if tenant has enabled a specific WHO selection.
     */
    public boolean isWhoSelectionEnabled(String code) {
        return tenantWhoSelectionRepository
                .findByWho_Code(code)
                .map(TenantWhoSelection::getActive)
                .orElse(false);
    }
}