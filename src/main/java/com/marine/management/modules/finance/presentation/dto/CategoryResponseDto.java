package com.marine.management.modules.finance.presentation.dto;

import com.marine.management.modules.finance.domain.entities.FinancialCategory;
import com.marine.management.modules.finance.domain.enums.RecordType;

import java.util.UUID;

public record CategoryResponseDto(
        UUID id,
        String name,
        String nameEn,
        RecordType categoryType,
        String description,
        boolean enabled,
        Integer displayOrder,
        String createdAt,
        boolean technical
) {
    public static CategoryResponseDto from(FinancialCategory category) {
        return new CategoryResponseDto(
                category.getId(),
                category.getName(),
                category.getNameEn(),
                category.getCategoryType(),
                category.getDescription(),
                category.isEnabled(),
                category.getDisplayOrder(),
                category.getCreatedAt().toString(),
                category.isTechnical()
        );
    }
}