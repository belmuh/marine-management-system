package com.marine.management.modules.finance.presentation;

import com.marine.management.modules.finance.application.ReferenceDataService;
import com.marine.management.modules.finance.presentation.dto.ReferenceDropdownData;
import com.marine.management.modules.finance.presentation.dto.controller.TenantMainCategoryDto;
import com.marine.management.modules.finance.presentation.dto.controller.TenantWhoSelectionDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Reference data controller for tenant-specific selections.
 *
 * DESIGN:
 * - All endpoints return tenant-filtered data (automatic via Hibernate filter)
 * - Only active selections are exposed to UI
 * - Used for dropdowns, filters, and form selections
 */
@RestController
@RequestMapping("/api/finance/reference")
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;

    public ReferenceDataController(ReferenceDataService referenceDataService) {
        this.referenceDataService = referenceDataService;
    }

    // ============================================
    // MAIN CATEGORY ENDPOINTS (Tenant-Filtered)
    // ============================================

    /**
     * Get all active main categories for current tenant.
     * Returns categories tenant has enabled (e.g., FUEL, CREW_EXPENSES).
     */
    @GetMapping("/main-categories")
    public ResponseEntity<List<TenantMainCategoryDto>> getActiveMainCategories() {
        var categories = referenceDataService.getActiveMainCategories();
        return ResponseEntity.ok(
                categories.stream()
                        .map(TenantMainCategoryDto::from)
                        .toList()
        );
    }

    /**
     * Get tenant main category by ID.
     */
    @GetMapping("/main-categories/{id}")
    public ResponseEntity<TenantMainCategoryDto> getTenantMainCategoryById(@PathVariable Long id) {
        return referenceDataService.getTenantMainCategoryById(id)
                .map(TenantMainCategoryDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get active main categories by type.
     * @param isTechnical true for technical (FUEL, MAINTENANCE), false for personal (CREW, PROVISIONS)
     */
    @GetMapping("/main-categories/by-type")
    public ResponseEntity<List<TenantMainCategoryDto>> getActiveMainCategoriesByType(
            @RequestParam boolean isTechnical
    ) {
        var categories = referenceDataService.getActiveMainCategoriesByType(isTechnical);
        return ResponseEntity.ok(
                categories.stream()
                        .map(TenantMainCategoryDto::from)
                        .toList()
        );
    }

    // ============================================
    // WHO ENDPOINTS (Tenant-Filtered)
    // ============================================

    /**
     * Get all active WHO selections for current tenant.
     * Returns WHO choices tenant has enabled (e.g., CAPTAIN, MAIN_ENGINE).
     */
    @GetMapping("/who")
    public ResponseEntity<List<TenantWhoSelectionDto>> getActiveWhoSelections() {
        var whoList = referenceDataService.getActiveWhoSelections();
        return ResponseEntity.ok(
                whoList.stream()
                        .map(TenantWhoSelectionDto::from)
                        .toList()
        );
    }

    /**
     * Get tenant WHO selection by ID.
     */
    @GetMapping("/who/{id}")
    public ResponseEntity<TenantWhoSelectionDto> getTenantWhoSelectionById(@PathVariable Long id) {
        return referenceDataService.getTenantWhoSelectionById(id)
                .map(TenantWhoSelectionDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get active WHO selections by type.
     * @param isTechnical true for equipment (MAIN_ENGINE, GENERATOR), false for people (CAPTAIN, CREW)
     */
    @GetMapping("/who/by-type")
    public ResponseEntity<List<TenantWhoSelectionDto>> getActiveWhoSelectionsByType(
            @RequestParam boolean isTechnical
    ) {
        var whoList = referenceDataService.getActiveWhoSelectionsByType(isTechnical);
        return ResponseEntity.ok(
                whoList.stream()
                        .map(TenantWhoSelectionDto::from)
                        .toList()
        );
    }

    /**
     * Get WHO selections suggested for a specific main category.
     * Example: FUEL → [MAIN_ENGINE, GENERATOR, TENDER]
     *
     * @param mainCategoryId Tenant main category ID (not global main category ID)
     */
    @GetMapping("/who/by-main-category/{mainCategoryId}")
    public ResponseEntity<List<TenantWhoSelectionDto>> getWhoSelectionsByMainCategory(
            @PathVariable Long mainCategoryId
    ) {
        var whoList = referenceDataService.getWhoSelectionsBySuggestedMainCategory(mainCategoryId);
        return ResponseEntity.ok(
                whoList.stream()
                        .map(TenantWhoSelectionDto::from)
                        .toList()
        );
    }

    // ============================================
    // COMBINED ENDPOINTS (for UI convenience)
    // ============================================

    /**
     * Get all reference data for dropdowns in one request.
     * Reduces frontend API calls - useful for form initialization.
     */
    @GetMapping("/dropdown-data")
    public ResponseEntity<ReferenceDropdownData> getDropdownData() {
        var mainCategories = referenceDataService.getActiveMainCategories()
                .stream()
                .map(TenantMainCategoryDto::from)
                .toList();

        var whoSelections = referenceDataService.getActiveWhoSelections()
                .stream()
                .map(TenantWhoSelectionDto::from)
                .toList();

        return ResponseEntity.ok(new ReferenceDropdownData(mainCategories, whoSelections));
    }

    /**
     * Get reference data filtered by technical flag.
     * Returns technical or personal choices based on expense type.
     *
     * @param isTechnical true for technical expenses (FUEL, MAINTENANCE), false for personal (CREW)
     */
    @GetMapping("/dropdown-data/by-type")
    public ResponseEntity<ReferenceDropdownData> getDropdownDataByType(
            @RequestParam boolean isTechnical
    ) {
        var mainCategories = referenceDataService.getActiveMainCategoriesByType(isTechnical)
                .stream()
                .map(TenantMainCategoryDto::from)
                .toList();

        var whoSelections = referenceDataService.getActiveWhoSelectionsByType(isTechnical)
                .stream()
                .map(TenantWhoSelectionDto::from)
                .toList();

        return ResponseEntity.ok(new ReferenceDropdownData(mainCategories, whoSelections));
    }
}