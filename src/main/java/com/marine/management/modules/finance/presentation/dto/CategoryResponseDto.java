package com.marine.management.modules.finance.presentation.dto;

import com.marine.management.modules.finance.domain.entity.FinancialCategory;
import com.marine.management.modules.finance.domain.enums.RecordType;

import java.util.UUID;

public record CategoryResponseDto(UUID id,
                                  String code,
                                  String name,
                                  RecordType categoryType,
                                  String description,
                                  boolean active,
                                  Integer displayOrder,
                                  String createdAt,
                                  Boolean isTechnical) {
    public static CategoryResponseDto from(FinancialCategory category) {
        return new CategoryResponseDto(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getCategoryType(),
                category.getDescription(),
                category.isActive(),
                category.getDisplayOrder(),
                category.getCreatedAt().toString(),
                category.isTechnical()
        );
    }
}
