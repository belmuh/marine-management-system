package com.marine.management.modules.finance.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Projection for pivot report - monthly breakdown
 */
public record PivotReportProjection(
        Long mainCategoryId,
        String mainCategoryCode,
        String mainCategoryNameTr,
        String mainCategoryNameEn,
        Boolean mainCategoryTechnical,
        UUID categoryId,
        String categoryCode,
        String categoryName,
        Boolean categoryTechnical,
        Long whoId,
        String whoCode,
        String whoNameTr,
        String whoNameEn,
        Boolean whoTechnical,
        Integer month,           // ⭐ Aylık breakdown için
        BigDecimal totalAmount
) {}
