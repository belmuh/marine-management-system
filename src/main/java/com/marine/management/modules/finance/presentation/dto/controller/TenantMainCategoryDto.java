package com.marine.management.modules.finance.presentation.dto.controller;

import com.marine.management.modules.finance.domain.entity.TenantMainCategory;

import java.util.UUID;

/**
 * DTO for tenant-enabled main categories.
 */
public record TenantMainCategoryDto(
        UUID id,                    // TenantMainCategory ID
        Long mainCategoryId,        // Global MainCategory ID
        String code,                // e.g., "FUEL", "CREW_EXPENSES"
        String nameTr,
        String nameEn,
        Boolean technical,
        Boolean active,             // Tenant-specific active flag
        Integer displayOrder,
        String budgetGuidelineMin,
        String budgetGuidelineMax
) {
    public static TenantMainCategoryDto from(TenantMainCategory tenantMainCategory) {
        var mainCategory = tenantMainCategory.getMainCategory();

        return new TenantMainCategoryDto(
                tenantMainCategory.getId(),
                mainCategory.getId(),
                mainCategory.getCode(),
                mainCategory.getNameTr(),
                mainCategory.getNameEn(),
                mainCategory.getTechnical(),
                tenantMainCategory.getActive(),
                mainCategory.getDisplayOrder(),
                mainCategory.getBudgetGuidelineMin(),
                mainCategory.getBudgetGuidelineMax()
        );
    }
}