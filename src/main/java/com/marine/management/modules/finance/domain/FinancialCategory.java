package com.marine.management.modules.finance.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "financial_categories")
public class FinancialCategory {
    private static final String CODE_PATTERN = "^[A-Z0-9_]+$";
    private static final int MAX_NAME_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(name = "display_order")
    private Integer displayOrder; // UI sıralama

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected FinancialCategory() {}

    // create
    public static FinancialCategory create(
            String code,
            String name,
            String description,
            Integer displayOrder
    ) {
        FinancialCategory category = new FinancialCategory();
        category.code = code.toUpperCase().trim();
        category.name = name.trim();
        category.description = description != null ? description.trim() : "";;
        category.displayOrder = displayOrder;
        category.createdAt = LocalDateTime.now();
        category.validate();
        return category;
    }

    // update
    public void updateDetails(String name, String description) {
        this.name = name.trim();
        this.description = description != null ? description.trim() : "";;
        validate();
    }

    public void changeDisplayOrder(Integer newOrder) {
        this.displayOrder = newOrder;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    // getters

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return isActive;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

   //validate
    private void validate() {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalStateException("Category code cannot be empty");
        }
        if (!code.matches(CODE_PATTERN)) {
            throw new IllegalStateException("Category code must contain only uppercase letters, numbers and underscores");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalStateException("Category name cannot be empty");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalStateException("Category name cannot exceed 100 characters");
        }
    }

    // EQUALS/HASHCODE
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FinancialCategory)) return false;
        FinancialCategory that = (FinancialCategory) o;
        return code != null && code.equals(that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return String.format(
                "FinancialCategory{id=%s, code='%s', name='%s', active=%s}",
                id, code, name, isActive
        );
    }
}
