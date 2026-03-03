package com.marine.management.modules.finance.domain.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * WHO entity for expense/income attribution (global reference data).
 *
 * DESIGN: NOT tenant-isolated (shared across all tenants)
 * - ISS standard WHO list (Captain, Crew, Main Engine, etc.)
 * - TenantWhoSelection links tenants to these WHO entries
 */
@Entity
@Table(name = "who")
public class Who {

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
    private boolean technical = true;  //  Field name without 'is' prefix

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "suggested_main_category_id")
    private Long suggestedMainCategoryId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Who() {}


    public static Who create(
            String code,
            String nameTr,
            String nameEn,
            boolean technical,
            Integer displayOrder,
            Long suggestedMainCategoryId
    ) {
        Who who = new Who();
        who.code = Objects.requireNonNull(code, "Code cannot be null").toUpperCase();
        who.nameTr = Objects.requireNonNull(nameTr, "Turkish name cannot be null");
        who.nameEn = Objects.requireNonNull(nameEn, "English name cannot be null");
        who.technical = technical;  //  Consistent field name
        who.displayOrder = displayOrder;
        who.suggestedMainCategoryId = suggestedMainCategoryId;

        who.validate();
        return who;
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

    public void updateDetails(String nameTr, String nameEn, boolean technical) {
        this.nameTr = Objects.requireNonNull(nameTr, "Turkish name cannot be null");
        this.nameEn = Objects.requireNonNull(nameEn, "English name cannot be null");
        this.technical = technical;  //  Consistent field name
        validate();
    }

    public void updateSuggestedCategory(Long mainCategoryId) {
        this.suggestedMainCategoryId = mainCategoryId;
    }

    // === GETTERS & SETTERS ===

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNameTr() {
        return nameTr;
    }

    public void setNameTr(String nameTr) {
        this.nameTr = nameTr;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public boolean isTechnical() {  //  Getter with 'is' prefix
        return technical;
    }

    public void setTechnical(boolean technical) {  //  Setter without 'is' prefix
        this.technical = technical;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Long getSuggestedMainCategoryId() {
        return suggestedMainCategoryId;
    }

    public void setSuggestedMainCategoryId(Long suggestedMainCategoryId) {
        this.suggestedMainCategoryId = suggestedMainCategoryId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Who who)) return false;
        return Objects.equals(id, who.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Who{id=%d, code='%s', nameEn='%s'}", id, code, nameEn);
    }
}