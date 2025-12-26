package com.marine.management.modules.finance.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TreeNode {
    private final Integer level;
    private final NodeType type;
    private final String id;  // String olarak sakla (Long/UUID her ikisi de string'e dönüşebilir)
    private final String name;
    private final String nameEn;
    private final BigDecimal amount;
    private final BigDecimal percentage;
    private final Boolean isTechnical;
    private final Integer childCount;
    private final List<TreeNode> children;

    private TreeNode(Builder builder) {
        this.level = builder.level;
        this.type = builder.type;
        this.id = builder.id;
        this.name = builder.name;
        this.nameEn = builder.nameEn;
        this.amount = builder.amount;
        this.percentage = builder.percentage;
        this.isTechnical = builder.isTechnical;
        this.childCount = builder.childCount;
        this.children = builder.children;
    }

    public enum NodeType {
        MAIN_CATEGORY,
        CATEGORY,
        WHO
    }

    // Getters
    public Integer getLevel() { return level; }
    public NodeType getType() { return type; }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getNameEn() { return nameEn; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getPercentage() { return percentage; }
    public Boolean getIsTechnical() { return isTechnical; }
    public Integer getChildCount() { return childCount; }
    public List<TreeNode> getChildren() { return children; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer level;
        private NodeType type;
        private String id;
        private String name;
        private String nameEn;
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal percentage = BigDecimal.ZERO;
        private Boolean isTechnical;
        private Integer childCount = 0;
        private List<TreeNode> children = new ArrayList<>();

        public Builder level(Integer level) {
            this.level = level;
            return this;
        }

        public Builder type(NodeType type) {
            this.type = type;
            return this;
        }

        // String ID
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        // Long ID (MainCategory, Who)
        public Builder id(Long id) {
            this.id = id != null ? id.toString() : null;
            return this;
        }

        // UUID ID (Category)
        public Builder id(UUID id) {
            this.id = id != null ? id.toString() : null;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder nameEn(String nameEn) {
            this.nameEn = nameEn;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder percentage(BigDecimal percentage) {
            this.percentage = percentage;
            return this;
        }

        public Builder isTechnical(Boolean isTechnical) {
            this.isTechnical = isTechnical;
            return this;
        }

        public Builder childCount(Integer childCount) {
            this.childCount = childCount;
            return this;
        }

        public Builder children(List<TreeNode> children) {
            this.children = children;
            this.childCount = children != null ? children.size() : 0;
            return this;
        }

        public TreeNode build() {
            return new TreeNode(this);
        }
    }
}