package com.marine.management.modules.finance.presentation.dto;

import com.marine.management.modules.finance.domain.entities.FinancialCategory;
import com.marine.management.modules.finance.domain.enums.RecordType;

import java.util.UUID;

public record CategoryResponseDto(
        UUID id,
        String code,
        String name,
        RecordType categoryType,
        String description,
        boolean enabled,        // Değişti: active → enabled
        Integer displayOrder,
        String createdAt,
        boolean technical       // Değişti: Boolean isTechnical → boolean technical
) {
    public static CategoryResponseDto from(FinancialCategory category) {
        return new CategoryResponseDto(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getCategoryType(),
                category.getDescription(),
                category.isEnabled(),      // Değişti: isActive() → isEnabled()
                category.getDisplayOrder(),
                category.getCreatedAt().toString(),
                category.isTechnical()
        );
    }
}