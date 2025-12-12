package com.marine.management.modules.finance.presentation;

import com.marine.management.modules.finance.application.ReferenceDataService;
import com.marine.management.modules.finance.presentation.dto.controller.MainCategoryDto;
import com.marine.management.modules.finance.presentation.dto.controller.WhoDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<MainCategoryDto>> getAllMainCategories() {
        var categories = referenceDataService.getActiveMainCategories();
        return ResponseEntity.ok(
                categories.stream()
                        .map(MainCategoryDto::from)
                        .toList()
        );
    }

    @GetMapping("/main-categories/{id}")
    public ResponseEntity<MainCategoryDto> getMainCategoryById(@PathVariable Long id) {
        return referenceDataService.getMainCategoryById(id)
                .map(MainCategoryDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/main-categories/by-type")
    public ResponseEntity<List<MainCategoryDto>> getMainCategoriesByType(
            @RequestParam boolean isTechnical
    ) {
        var categories = referenceDataService.getMainCategoriesByType(isTechnical);
        return ResponseEntity.ok(
                categories.stream()
                        .map(MainCategoryDto::from)
                        .toList()
        );
    }

    // ============================================
    // WHO ENDPOINTS
    // ============================================

    @GetMapping("/who")
    public ResponseEntity<List<WhoDto>> getAllWho() {
        var whoList = referenceDataService.getActiveWho();
        return ResponseEntity.ok(
                whoList.stream()
                        .map(WhoDto::from)
                        .toList()
        );
    }

    @GetMapping("/who/{id}")
    public ResponseEntity<WhoDto> getWhoById(@PathVariable Long id) {
        return referenceDataService.getWhoById(id)
                .map(WhoDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/who/by-type")
    public ResponseEntity<List<WhoDto>> getWhoByType(
            @RequestParam boolean isTechnical
    ) {
        var whoList = referenceDataService.getWhoByType(isTechnical);
        return ResponseEntity.ok(
                whoList.stream()
                        .map(WhoDto::from)
                        .toList()
        );
    }

    @GetMapping("/who/by-main-category/{mainCategoryId}")
    public ResponseEntity<List<WhoDto>> getWhoByMainCategory(
            @PathVariable Long mainCategoryId
    ) {
        var whoList = referenceDataService.getWhoBySuggestedMainCategory(mainCategoryId);
        return ResponseEntity.ok(
                whoList.stream()
                        .map(WhoDto::from)
                        .toList()
        );
    }

    // ============================================
    // COMBINED ENDPOINTS (for UI convenience)
    // ============================================

    @GetMapping("/dropdown-data")
    public ResponseEntity<ReferenceDropdownData> getDropdownData() {
        var mainCategories = referenceDataService.getActiveMainCategories()
                .stream()
                .map(MainCategoryDto::from)
                .toList();

        var whoList = referenceDataService.getActiveWho()
                .stream()
                .map(WhoDto::from)
                .toList();

        return ResponseEntity.ok(new ReferenceDropdownData(mainCategories, whoList));
    }
}

// ============================================
// RESPONSE DTO
// ============================================

record ReferenceDropdownData(
        List<MainCategoryDto> mainCategories,
        List<WhoDto> whoList
) {}