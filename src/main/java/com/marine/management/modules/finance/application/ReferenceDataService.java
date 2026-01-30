package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.domain.entity.TenantMainCategory;
import com.marine.management.modules.finance.domain.entity.TenantWhoSelection;
import com.marine.management.modules.finance.infrastructure.TenantMainCategoryRepository;
import com.marine.management.modules.finance.infrastructure.TenantWhoSelectionRepository;
import com.marine.management.modules.finance.presentation.dto.controller.TenantMainCategoryDto;
import com.marine.management.modules.finance.presentation.dto.controller.TenantWhoSelectionDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Reference data service for tenant-specific selections.
 *
 * DESIGN:
 * - Returns DTOs to prevent LazyInitializationException
 * - Uses JOIN FETCH to avoid N+1 queries
 * - Automatically filters by tenant_id via @Filter(tenantFilter)
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
     * Get enabled main categories for current tenant.
     *
     * Returns: enabled=true AND is_deleted=false AND tenant_id=current
     * Optimized: Single query with JOIN FETCH, returns DTOs
     */
    public List<TenantMainCategoryDto> getEnabledMainCategories() {
        return tenantMainCategoryRepository.findAllActiveWithMainCategory()
                .stream()
                .map(TenantMainCategoryDto::from)
                .toList();
    }

    /**
     * Get enabled main categories by technical flag.
     * @param technical true for technical (e.g. FUEL, MAINTENANCE), false for personal (e.g. CREW)
     */
    public List<TenantMainCategoryDto> getEnabledMainCategoriesByType(boolean technical) {
        return tenantMainCategoryRepository.findByEnabledTrueAndIsTechnicalWithMainCategory(technical)
                .stream()
                .map(TenantMainCategoryDto::from)
                .toList();
    }

    /**
     * Get tenant main category by ID.
     *
     * Returns: Only if is_deleted=false AND tenant_id=current
     */
    public Optional<TenantMainCategoryDto> getTenantMainCategoryById(UUID id) {
        return tenantMainCategoryRepository.findById(id)
                .map(TenantMainCategoryDto::from);
    }

    /**
     * Get tenant main category by global main category code.
     * Useful for onboarding/initialization.
     */
    public Optional<TenantMainCategoryDto> getTenantMainCategoryByCode(String code) {
        return tenantMainCategoryRepository.findByMainCategoryCodeWithMainCategory(code)
                .map(TenantMainCategoryDto::from);
    }

    // ============================================
    // WHO QUERIES (Tenant-Filtered)
    // ============================================

    /**
     * Get enabled WHO selections for current tenant.
     *
     * Returns: enabled=true AND is_deleted=false AND tenant_id=current
     * Optimized: Single query with JOIN FETCH, returns DTOs
     */
    public List<TenantWhoSelectionDto> getEnabledWhoSelections() {
        return tenantWhoSelectionRepository.findAllActiveWithWho()
                .stream()
                .map(TenantWhoSelectionDto::from)
                .toList();
    }

    /**
     * Get enabled WHO selections by technical flag.
     * @param technical true for equipment (e.g. MAIN_ENGINE), false for people (e.g. CAPTAIN)
     */
    public List<TenantWhoSelectionDto> getEnabledWhoSelectionsByType(boolean technical) {
        return tenantWhoSelectionRepository.findByEnabledTrueAndTechnicalWithWho(technical)
                .stream()
                .map(TenantWhoSelectionDto::from)
                .toList();
    }

    /**
     * Get WHO selections suggested for a specific main category.
     * Example: FUEL → [MAIN_ENGINE, GENERATOR, TENDER]
     */
    public List<TenantWhoSelectionDto> getWhoSelectionsBySuggestedMainCategory(Long mainCategoryId) {
        return tenantWhoSelectionRepository.findBySuggestedMainCategoryWithWho(mainCategoryId)
                .stream()
                .map(TenantWhoSelectionDto::from)
                .toList();
    }

    /**
     * Get tenant WHO selection by ID.
     *
     * Returns: Only if is_deleted=false AND tenant_id=current
     */
    public Optional<TenantWhoSelectionDto> getTenantWhoSelectionById(UUID id) {
        return tenantWhoSelectionRepository.findById(id)
                .map(TenantWhoSelectionDto::from);
    }

    /**
     * Get tenant WHO selection by global WHO code.
     * Useful for onboarding/initialization.
     */
    public Optional<TenantWhoSelectionDto> getTenantWhoSelectionByCode(String code) {
        return tenantWhoSelectionRepository.findByWhoCodeWithWho(code)
                .map(TenantWhoSelectionDto::from);
    }

    // ============================================
    // UTILITY METHODS (Entity kullanımı OK)
    // ============================================

    /**
     * Check if tenant has enabled a specific main category.
     * Note: Uses existence check, no JOIN FETCH needed
     */
    public boolean isMainCategoryEnabled(String code) {
        return tenantMainCategoryRepository
                .findByMainCategoryCodeWithMainCategory(code)
                .map(TenantMainCategory::isEnabled)
                .orElse(false);
    }

    /**
     * Check if tenant has enabled a specific WHO selection.
     * Note: Uses existence check, no JOIN FETCH needed
     */
    public boolean isWhoSelectionEnabled(String code) {
        return tenantWhoSelectionRepository
                .findByWhoCodeWithWho(code)
                .map(TenantWhoSelection::isEnabled)
                .orElse(false);
    }
}