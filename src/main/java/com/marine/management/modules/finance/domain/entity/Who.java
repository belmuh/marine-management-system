package com.marine.management.modules.finance.domain.entity;

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
    private Boolean technical = true;

    @Column(name = "suggested_main_category_id")
    private Long suggestedMainCategoryId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Who() {}

    /**
     * Creates a new WHO entry.
     *
     * @param code unique WHO code (e.g., "CAPTAIN", "MAIN_ENGINE")
     * @param nameTr Turkish name
     * @param nameEn English name
     * @param isTechnical technical equipment flag
     * @param suggestedMainCategoryId suggested main category (nullable)
     * @return new Who instance
     */
    public static Who create(
            String code,
            String nameTr,
            String nameEn,
            boolean isTechnical,
            Long suggestedMainCategoryId
    ) {
        Who who = new Who();
        who.code = Objects.requireNonNull(code, "Code cannot be null").toUpperCase();
        who.nameTr = Objects.requireNonNull(nameTr, "Turkish name cannot be null");
        who.nameEn = Objects.requireNonNull(nameEn, "English name cannot be null");
        who.technical = isTechnical;
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

    public void updateDetails(String nameTr, String nameEn, Boolean isTechnical) {
        this.nameTr = Objects.requireNonNull(nameTr, "Turkish name cannot be null");
        this.nameEn = Objects.requireNonNull(nameEn, "English name cannot be null");
        this.technical = Objects.requireNonNull(isTechnical, "Technical flag cannot be null");
        validate();
    }

    public void updateSuggestedCategory(Long mainCategoryId) {
        this.suggestedMainCategoryId = mainCategoryId;
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
    public Long getSuggestedMainCategoryId() { return suggestedMainCategoryId; }
    public void setSuggestedMainCategoryId(Long suggestedMainCategoryId) {
        this.suggestedMainCategoryId = suggestedMainCategoryId;
    }
    public LocalDateTime getCreatedAt() { return createdAt; }

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
