package com.marine.management.modules.finance.domain.enums;

/**
 * Types of nodes in the financial tree report hierarchy.
 *
 * <p>Tree structure:
 * <pre>
 * MAIN_CATEGORY (level 1)
 *   └─ CATEGORY (level 2)
 *       └─ WHO (level 3)
 * </pre>
 */
public enum NodeType {
    /**
     * Top-level grouping (e.g., "Operating Expenses", "Capital Expenditure")
     */
    MAIN_CATEGORY(1),

    /**
     * Mid-level grouping (e.g., "Fuel", "Maintenance", "Crew Salaries")
     */
    CATEGORY(2),

    /**
     * Leaf-level grouping by person or company (e.g., "Shell Marine", "John Doe")
     */
    WHO(3);

    private final int level;

    NodeType(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Returns the enum name as string (e.g., "MAIN_CATEGORY")
     * Used for DTO type field.
     */
    public String getTypeName() {
        return this.name();
    }
}