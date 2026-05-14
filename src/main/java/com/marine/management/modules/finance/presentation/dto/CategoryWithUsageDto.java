package com.marine.management.modules.finance.presentation.dto;

import com.marine.management.modules.finance.domain.entities.FinancialCategory;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.infrastructure.FinancialCategoryRepository;

import java.util.UUID;

public record CategoryWithUsageDto(
        UUID id,
        String name,
        String description,
        RecordType categoryType,
        boolean active,
        Integer displayOrder,
        Long usageCount
) {
    public static CategoryWithUsageDto from(FinancialCategoryRepository.CategoryWithUsageCount cwu) {
        FinancialCategory category = cwu.getCategory();
        return new CategoryWithUsageDto(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getCategoryType(),
                category.isEnabled(),
                category.getDisplayOrder(),
                cwu.getUsageCount()
        );
    }
}