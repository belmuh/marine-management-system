package com.marine.management.modules.finance.domain.model;

import java.math.BigDecimal;
import java.util.*;

public class PivotTreeNode {
    private final String id;
    private final Integer level;
    private final String type;  // MAIN_CATEGORY, CATEGORY, WHO
    private final String name;
    private final String nameEn;
    private final Boolean isTechnical;

    // Key: "2024-01", "2024-02", ..., "TOTAL"
    private final Map<String, BigDecimal> monthlyValues;
    private final List<PivotTreeNode> children;

    public PivotTreeNode(
            String id,
            Integer level,
            String type,
            String name,
            String nameEn,
            Boolean isTechnical,
            Map<String, BigDecimal> monthlyValues,
            List<PivotTreeNode> children
    ) {
        this.id = id;
        this.level = level;
        this.type = type;
        this.name = name;
        this.nameEn = nameEn;
        this.isTechnical = isTechnical;
        this.monthlyValues = monthlyValues;
        this.children = children;
    }

    // Getters
    public String getId() { return id; }
    public Integer getLevel() { return level; }
    public String getType() { return type; }
    public String getName() { return name; }
    public String getNameEn() { return nameEn; }
    public Boolean getIsTechnical() { return isTechnical; }
    public Map<String, BigDecimal> getMonthlyValues() { return monthlyValues; }
    public List<PivotTreeNode> getChildren() { return children; }
}
