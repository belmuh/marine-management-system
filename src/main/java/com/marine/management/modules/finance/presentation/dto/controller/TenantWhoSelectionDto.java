package com.marine.management.modules.finance.presentation.dto.controller;

import com.marine.management.modules.finance.domain.entity.TenantWhoSelection;

import java.util.UUID;

public record TenantWhoSelectionDto(
        UUID id,
        Long whoId,
        String code,
        String nameTr,
        String nameEn,
        Boolean technical,
        Boolean enabled,
        Integer displayOrder,        // Eklendi (UI'da sıralama için)
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
                who.isTechnical(),
                tenantWhoSelection.isEnabled(),
                who.getDisplayOrder(),           // Eklendi
                who.getSuggestedMainCategoryId()
        );
    }
}
