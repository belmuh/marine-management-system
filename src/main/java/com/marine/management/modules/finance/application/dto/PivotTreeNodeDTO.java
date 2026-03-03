package com.marine.management.modules.finance.application.dto;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Pivot tree node for hierarchical monthly breakdown.
 *
 * <p>Represents a node in the pivot report tree structure:
 * <ul>
 *   <li>Level 1: Main Category</li>
 *   <li>Level 2: Category</li>
 *   <li>Level 3: Who (Person/Company)</li>
 * </ul>
 *
 * @param id Category/MainCategory/Who ID
 * @param level Node level (1-3)
 * @param type Node type ("MAIN_CATEGORY", "CATEGORY", "WHO")
 * @param name Turkish name
 * @param nameEn English name
 * @param technical Technical category flag
 * @param monthlyValues Monthly amounts (e.g., "01" -> amount, "02" -> amount)
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
    /**
     * Compact constructor ensuring immutability and null-safety.
     */
    public PivotTreeNodeDTO {
        // Ensure immutability
        monthlyValues = monthlyValues != null
                ? Map.copyOf(monthlyValues)
                : Collections.emptyMap();

        children = children != null
                ? List.copyOf(children)
                : Collections.emptyList();
    }
}