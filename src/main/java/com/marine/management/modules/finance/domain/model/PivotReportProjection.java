package com.marine.management.modules.finance.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Projection for pivot report - monthly breakdown
 */
public record PivotReportProjection(
        Long mainCategoryId,
        String mainCategoryNameTr,
        String mainCategoryNameEn,
        Boolean mainCategoryTechnical,
        UUID categoryId,
        String categoryName,
        String categoryNameEn,
        Boolean categoryTechnical,
        Long whoId,
        String whoNameTr,
        String whoNameEn,
        Boolean whoTechnical,
        Integer month,
        BigDecimal totalAmount
) {}
