package com.marine.management.modules.finance.presentation.dto;

import com.marine.management.modules.finance.presentation.dto.controller.MainCategoryDto;
import com.marine.management.modules.finance.presentation.dto.controller.WhoDto;

import java.util.List;

public record ReferenceDropdownData(
        List<MainCategoryDto> mainCategories,
        List<WhoDto> whoList
) {
}
