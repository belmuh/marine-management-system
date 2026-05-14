package com.marine.management.modules.finance.presentation.dto.controller;

import com.marine.management.modules.finance.domain.entities.TenantWhoSelection;

import java.util.UUID;

public record TenantWhoSelectionDto(
        UUID id,
        Long whoId,
        String nameTr,
        String nameEn,
        Boolean technical,
        Boolean enabled,
        Integer displayOrder,
        Long suggestedMainCategoryId
) {
    public static TenantWhoSelectionDto from(TenantWhoSelection tenantWhoSelection) {
        var who = tenantWhoSelection.getWho();

        return new TenantWhoSelectionDto(
                tenantWhoSelection.getId(),
                who.getId(),
                who.getNameTr(),
                who.getNameEn(),
                who.isTechnical(),
                tenantWhoSelection.isEnabled(),
                who.getDisplayOrder(),
                who.getSuggestedMainCategoryId()
        );
    }
}
