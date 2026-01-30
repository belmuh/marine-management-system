package com.marine.management.modules.finance.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Pivot tree node for hierarchical monthly breakdown.
 *
 * @param id Category/MainCategory/Who ID
 * @param level 1=MainCategory, 2=Category, 3=Who
 * @param type "MAIN_CATEGORY", "CATEGORY", "WHO"
 * @param name Turkish name
 * @param nameEn English name
 * @param isTechnical Technical category flag
 * @param monthlyValues Monthly amounts ("01" -> amount, "02" -> amount, ...)
 * @param children Child nodes (empty for WHO level)
 */
public record PivotTreeNodeDTO(
        String id,
        Integer level,
        String type,
        String name,
        String nameEn,
        Boolean technical,
        Map<String, BigDecimal> monthlyValues,
        List<PivotTreeNodeDTO> children
) {
    // Compact constructor ile validation
    public PivotTreeNodeDTO {
        if (level < 1 || level > 3) {
            throw new IllegalArgumentException("Level must be 1-3");
        }
        if (!List.of("MAIN_CATEGORY", "CATEGORY", "WHO").contains(type)) {
            throw new IllegalArgumentException("Invalid type: " + type);
        }
        monthlyValues = Map.copyOf(monthlyValues);  // Immutability
        children = List.copyOf(children);
    }
}