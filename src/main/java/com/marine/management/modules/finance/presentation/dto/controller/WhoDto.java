package com.marine.management.modules.finance.presentation.dto.controller;

import com.marine.management.modules.finance.domain.entity.Who;

public record WhoDto(
        Long id,
        String code,
        String nameTr,
        String nameEn,
        Boolean technical,
        Long suggestedMainCategoryId,
        Boolean active
) {
    public static WhoDto from(Who who) {
        return new WhoDto(
                who.getId(),
                who.getCode(),
                who.getNameTr(),
                who.getNameEn(),
                who.getTechnical(),
                who.getSuggestedMainCategoryId(),
                who.getActive()
        );
    }
}