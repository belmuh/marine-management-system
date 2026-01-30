package com.marine.management.modules.finance.presentation;

import com.marine.management.modules.finance.application.ReferenceDataService;
import com.marine.management.modules.finance.presentation.dto.ReferenceDropdownData;
import com.marine.management.modules.finance.presentation.dto.controller.TenantMainCategoryDto;
import com.marine.management.modules.finance.presentation.dto.controller.TenantWhoSelectionDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Reference data controller for tenant-specific selections.
 *
 * DESIGN:
 * - Controller does NOT do DTO mapping
 * - Service layer returns DTOs
 * - Prevents LazyInitializationException
 * - Clean & thin controller
 */
@RestController
@RequestMapping("/api/finance/reference")
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;

    public ReferenceDataController(ReferenceDataService referenceDataService) {
        this.referenceDataService = referenceDataService;
    }

    // ============================================
    // MAIN CATEGORY ENDPOINTS
    // ============================================

    @GetMapping("/main-categories")
    public ResponseEntity<List<TenantMainCategoryDto>> getActiveMainCategories() {
        return ResponseEntity.ok(
                referenceDataService.getEnabledMainCategories()
        );
    }

    @GetMapping("/main-categories/{id}")
    public ResponseEntity<TenantMainCategoryDto> getTenantMainCategoryById(
            @PathVariable UUID id
    ) {
        return referenceDataService.getTenantMainCategoryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/main-categories/by-type")
    public ResponseEntity<List<TenantMainCategoryDto>> getActiveMainCategoriesByType(
            @RequestParam boolean isTechnical
    ) {
        return ResponseEntity.ok(
                referenceDataService.getEnabledMainCategoriesByType(isTechnical)
        );
    }

    // ============================================
    // WHO ENDPOINTS
    // ============================================

    @GetMapping("/who")
    public ResponseEntity<List<TenantWhoSelectionDto>> getActiveWhoSelections() {
        return ResponseEntity.ok(
                referenceDataService.getEnabledWhoSelections()
        );
    }

    @GetMapping("/who/{id}")
    public ResponseEntity<TenantWhoSelectionDto> getTenantWhoSelectionById(
            @PathVariable UUID id
    ) {
        return referenceDataService.getTenantWhoSelectionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/who/by-type")
    public ResponseEntity<List<TenantWhoSelectionDto>> getActiveWhoSelectionsByType(
            @RequestParam boolean isTechnical
    ) {
        return ResponseEntity.ok(
                referenceDataService.getEnabledWhoSelectionsByType(isTechnical)
        );
    }

    @GetMapping("/who/by-main-category/{mainCategoryId}")
    public ResponseEntity<List<TenantWhoSelectionDto>> getWhoSelectionsByMainCategory(
            @PathVariable Long mainCategoryId
    ) {
        return ResponseEntity.ok(
                referenceDataService.getWhoSelectionsBySuggestedMainCategory(mainCategoryId)
        );
    }

    // ============================================
    // COMBINED ENDPOINTS
    // ============================================

    @GetMapping("/dropdown-data")
    public ResponseEntity<ReferenceDropdownData> getDropdownData() {
        return ResponseEntity.ok(
                new ReferenceDropdownData(
                        referenceDataService.getEnabledMainCategories(),
                        referenceDataService.getEnabledWhoSelections()
                )
        );
    }

    @GetMapping("/dropdown-data/by-type")
    public ResponseEntity<ReferenceDropdownData> getDropdownDataByType(
            @RequestParam boolean isTechnical
    ) {
        return ResponseEntity.ok(
                new ReferenceDropdownData(
                        referenceDataService.getEnabledMainCategoriesByType(isTechnical),
                        referenceDataService.getEnabledWhoSelectionsByType(isTechnical)
                )
        );
    }
}
