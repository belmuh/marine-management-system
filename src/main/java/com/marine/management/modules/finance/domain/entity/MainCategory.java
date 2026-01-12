package com.marine.management.modules.finance.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Main category for financial classification (global reference data).
 *
 * DESIGN: NOT tenant-isolated (shared across all tenants)
 * - ISS standard categories
 * - TenantMainCategory links tenants to these categories
 */
@Entity
@Table(name = "main_categories")
public class MainCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name_tr", nullable = false, length = 100)
    private String nameTr;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @Column(name = "is_technical", nullable = false)
    private Boolean technical = true;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "budget_guideline_min", length = 10)
    private String budgetGuidelineMin;

    @Column(name = "budget_guideline_max", length = 10)
    private String budgetGuidelineMax;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected MainCategory() {}

    /**
     * Creates a new main category.
     *
     * @param code unique category code (e.g., "CREW_EXPENSES")
     * @param nameTr Turkish name
     * @param nameEn English name
     * @param isTechnical technical expense flag
     * @param displayOrder UI sort order
     * @param budgetMin budget guideline minimum
     * @param budgetMax budget guideline maximum
     * @return new MainCategory instance
     */
    public static MainCategory create(
            String code,
            String nameTr,
            String nameEn,
            boolean isTechnical,
            int displayOrder,
            String budgetMin,
            String budgetMax
    ) {
        MainCategory category = new MainCategory();
        category.code = Objects.requireNonNull(code, "Code cannot be null").toUpperCase();
        category.nameTr = Objects.requireNonNull(nameTr, "Turkish name cannot be null");
        category.nameEn = Objects.requireNonNull(nameEn, "English name cannot be null");
        category.technical = isTechnical;
        category.displayOrder = displayOrder;
        category.budgetGuidelineMin = budgetMin;
        category.budgetGuidelineMax = budgetMax;

        category.validate();
        return category;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    private void validate() {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Code cannot be empty");
        }
        if (code.length() > 50) {
            throw new IllegalArgumentException("Code cannot exceed 50 characters");
        }
        if (nameTr == null || nameTr.trim().isEmpty()) {
            throw new IllegalArgumentException("Turkish name cannot be empty");
        }
        if (nameEn == null || nameEn.trim().isEmpty()) {
            throw new IllegalArgumentException("English name cannot be empty");
        }
    }

    // === BUSINESS METHODS ===

    public void updateDetails(String nameTr, String nameEn, Boolean isTechnical) {
        this.nameTr = Objects.requireNonNull(nameTr, "Turkish name cannot be null");
        this.nameEn = Objects.requireNonNull(nameEn, "English name cannot be null");
        this.technical = Objects.requireNonNull(isTechnical, "Technical flag cannot be null");
        validate();
    }

    public void updateBudgetGuidelines(String min, String max) {
        this.budgetGuidelineMin = min;
        this.budgetGuidelineMax = max;
    }

    // === GETTERS & SETTERS ===

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNameTr() { return nameTr; }
    public void setNameTr(String nameTr) { this.nameTr = nameTr; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public Boolean getTechnical() { return technical; }
    public void setTechnical(Boolean technical) { this.technical = technical; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public String getBudgetGuidelineMin() { return budgetGuidelineMin; }
    public void setBudgetGuidelineMin(String budgetGuidelineMin) { this.budgetGuidelineMin = budgetGuidelineMin; }
    public String getBudgetGuidelineMax() { return budgetGuidelineMax; }
    public void setBudgetGuidelineMax(String budgetGuidelineMax) { this.budgetGuidelineMax = budgetGuidelineMax; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MainCategory that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("MainCategory{id=%d, code='%s', nameEn='%s'}", id, code, nameEn);
    }
}