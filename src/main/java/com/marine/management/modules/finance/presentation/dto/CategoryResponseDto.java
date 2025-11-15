package com.marine.management.modules.finance.presentation.dto;

import com.marine.management.modules.finance.domain.FinancialCategory;

import java.util.UUID;

public record CategoryResponseDto(UUID id,
                                  String code,
                                  String name,
                                  String description,
                                  boolean active,
                                  Integer displayOrder,
                                  String createdAt) {
    public static CategoryResponseDto from(FinancialCategory category) {
        return new CategoryResponseDto(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getDescription(),
                category.isActive(),
                category.getDisplayOrder(),
                category.getCreatedAt().toString()
        );
    }
}
