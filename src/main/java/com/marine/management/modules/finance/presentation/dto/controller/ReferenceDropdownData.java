package com.marine.management.modules.finance.presentation.dto.controller;

import com.marine.management.modules.finance.presentation.dto.controller.TenantWhoSelectionDto;

import java.util.List;

/**
 * Combined reference data for UI dropdowns.
 * Reduces frontend API calls.
 */
public record ReferenceDropdownData(
        List<TenantMainCategoryDto> mainCategories,
        List<TenantWhoSelectionDto> whoSelections
) {}
