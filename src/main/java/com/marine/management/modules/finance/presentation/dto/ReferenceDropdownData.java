package com.marine.management.modules.finance.presentation.dto;

import com.marine.management.modules.finance.presentation.dto.controller.TenantMainCategoryDto;
import com.marine.management.modules.finance.presentation.dto.controller.TenantWhoSelectionDto;

import java.util.List;

public record ReferenceDropdownData(
        List<TenantMainCategoryDto> mainCategories,
        List<TenantWhoSelectionDto> whoList
) {
}
