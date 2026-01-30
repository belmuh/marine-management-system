package com.marine.management.modules.finance.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Projection for tree report - flat result from database
 * Single query with all necessary data for tree building
 */
public record TreeReportProjection(
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
        BigDecimal totalAmount
) {}