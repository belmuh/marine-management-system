package com.marine.management.modules.finance.presentation.dto;

import com.marine.management.modules.finance.domain.entity.FinancialCategory;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.infrastructure.FinancialCategoryRepository;

import java.util.UUID;

public record CategoryWithUsageDto(
        UUID id,
        String code,
        String name,
        String description,
        RecordType categoryType,
        boolean isActive,
        Integer displayOrder,
        Long usageCount
) {
    public static CategoryWithUsageDto from(FinancialCategoryRepository.CategoryWithUsageCount cwu) {
        FinancialCategory category = cwu.getCategory();
        return new CategoryWithUsageDto(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getDescription(),
                category.getCategoryType(),
                category.isActive(),
                category.getDisplayOrder(),
                cwu.getUsageCount()
        );
    }
}