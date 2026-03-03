package com.marine.management.modules.finance.application.dto;

import com.marine.management.modules.finance.domain.enums.NodeType;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Tree node representing a level in financial hierarchy.
 *
 * <p>Supports three levels:
 * <ul>
 *   <li>Level 1: Main Category - Top-level grouping</li>
 *   <li>Level 2: Category - Mid-level grouping</li>
 *   <li>Level 3: Who - Leaf-level (Person/Company)</li>
 * </ul>
 *
 * @param level Node level (1-3)
 * @param type Node type identifier
 * @param id Unique identifier
 * @param name Turkish name
 * @param nameEn English name
 * @param amount Total amount for this node
 * @param percentage Percentage of parent total
 * @param technical Technical category flag
 * @param childCount Number of child nodes
 * @param children Child nodes (empty for WHO level)
 */
public record TreeNodeDTO(
        Integer level,
        String type,
        String id,
        String name,
        String nameEn,
        BigDecimal amount,
        BigDecimal percentage,
        Boolean technical,
        Integer childCount,
        List<TreeNodeDTO> children
) {

    /**
     * Compact constructor with null-safe default for children.
     */
    public TreeNodeDTO {
        if (children == null) {
            children = Collections.emptyList();
        }
    }

    /**
     * Creates an unassigned main category node.
     *
     * Used when financial entries don't have a main category reference.
     *
     * @param amount Total amount for unassigned entries
     * @param percentage Percentage of grand total
     * @param children Child category nodes
     * @return Unassigned main category node
     */
    public static TreeNodeDTO unassignedMainCategory(
            BigDecimal amount,
            BigDecimal percentage,
            List<TreeNodeDTO> children) {

        List<TreeNodeDTO> safeChildren = children != null
                ? children
                : Collections.emptyList();

        return new TreeNodeDTO(
                NodeType.MAIN_CATEGORY.getLevel(),
                NodeType.MAIN_CATEGORY.getTypeName(),
                "-1",
                "Atanmamış",
                "Unassigned",
                amount,
                percentage,
                null,
                safeChildren.size(),
                safeChildren
        );
    }

    /**
     * Creates an unspecified WHO node.
     *
     * Used when financial entries don't have a WHO (person/company) reference.
     *
     * @param amount Total amount for unspecified who
     * @param percentage Percentage of parent category total
     * @return Unspecified WHO node (leaf node, no children)
     */
    public static TreeNodeDTO unspecifiedWho(
            BigDecimal amount,
            BigDecimal percentage) {
        return new TreeNodeDTO(
                NodeType.WHO.getLevel(),
                NodeType.WHO.getTypeName(),
                "-1",
                "Belirtilmemiş",
                "Unspecified",
                amount,
                percentage,
                null,
                0,
                Collections.emptyList()
        );
    }
}