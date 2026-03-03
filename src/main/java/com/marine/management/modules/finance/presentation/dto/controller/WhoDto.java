package com.marine.management.modules.finance.presentation.dto.controller;

import com.marine.management.modules.finance.domain.entities.Who;

/**
 * DTO for Who entity (global reference data).
 *
 * NOTE: No 'active' field - these are ISS standard WHO entries.
 * Tenants enable/disable via TenantWhoSelection link table.
 */
public record WhoDto(
        Long id,
        String code,
        String nameTr,
        String nameEn,
        Boolean isTechnical,
        Long suggestedMainCategoryId
) {
    public static WhoDto from(Who who) {
        return new WhoDto(
                who.getId(),
                who.getCode(),
                who.getNameTr(),
                who.getNameEn(),
                who.isTechnical(),
                who.getSuggestedMainCategoryId()
        );
    }
}