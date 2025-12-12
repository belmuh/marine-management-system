package com.marine.management.modules.finance.presentation.dto.controller;

import com.marine.management.modules.finance.domain.entity.MainCategory;

public record MainCategoryDto(
        Long id,
        String code,
        String nameTr,
        String nameEn,
        Boolean isTechnical,
        Boolean active
) {
    public static MainCategoryDto from(MainCategory mainCategory) {
        return new MainCategoryDto(
                mainCategory.getId(),
                mainCategory.getCode(),
                mainCategory.getNameTr(),
                mainCategory.getNameEn(),
                mainCategory.getTechnical(),
                mainCategory.getActive()
        );
    }
}