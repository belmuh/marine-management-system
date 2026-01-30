package com.marine.management.modules.finance.presentation.dto.controller;

import com.marine.management.modules.finance.domain.entity.MainCategory;

/**
 * DTO for MainCategory (global reference data).
 *
 * NOTE: No 'active' field - these are ISS standard categories.
 * Tenants enable/disable via TenantMainCategory link table.
 */
public record MainCategoryDto(
        Long id,
        String code,
        String nameTr,
        String nameEn,
        Boolean isTechnical,
        Integer displayOrder,
        String budgetGuidelineMin,
        String budgetGuidelineMax
) {
    public static MainCategoryDto from(MainCategory mainCategory) {
        return new MainCategoryDto(
                mainCategory.getId(),
                mainCategory.getCode(),
                mainCategory.getNameTr(),
                mainCategory.getNameEn(),
                mainCategory.isTechnical(),
                mainCategory.getDisplayOrder(),
                mainCategory.getBudgetGuidelineMin(),
                mainCategory.getBudgetGuidelineMax()
        );
    }
}