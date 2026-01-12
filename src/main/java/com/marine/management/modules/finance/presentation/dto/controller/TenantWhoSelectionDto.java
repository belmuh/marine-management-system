package com.marine.management.modules.finance.presentation.dto.controller;

import com.marine.management.modules.finance.domain.entity.TenantWhoSelection;

import java.util.UUID;

/**
 * DTO for tenant-enabled WHO selections.
 */
public record TenantWhoSelectionDto(
        UUID id,                    // TenantWhoSelection ID
        Long whoId,                 // Global Who ID
        String code,                // e.g., "CAPTAIN", "MAIN_ENGINE"
        String nameTr,
        String nameEn,
        Boolean technical,
        Boolean active,             // Tenant-specific active flag
        Long suggestedMainCategoryId
) {
    public static TenantWhoSelectionDto from(TenantWhoSelection tenantWhoSelection) {
        var who = tenantWhoSelection.getWho();

        return new TenantWhoSelectionDto(
                tenantWhoSelection.getId(),
                who.getId(),
                who.getCode(),
                who.getNameTr(),
                who.getNameEn(),
                who.getTechnical(),
                tenantWhoSelection.getActive(),
                who.getSuggestedMainCategoryId()
        );
    }
}
