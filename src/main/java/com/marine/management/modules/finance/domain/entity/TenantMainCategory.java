package com.marine.management.modules.finance.domain.entity;

import com.marine.management.shared.domain.BaseTenantEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Tenant-specific main category selection and customization.
 *
 * TENANT ISOLATION:
 * - Extends BaseTenantEntity (auto tenant_id injection)
 * - Links tenant to global MainCategory
 * - Tenant can customize budget and accounting code
 *
 * BUSINESS RULES:
 * - Tenant selects which MainCategories to use
 * - Same MainCategory can be selected by multiple tenants
 * - Each tenant has unique budget/accounting code per category
 */
@Entity
@Table(
        name = "tenant_main_categories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_tenant_main_category",
                        columnNames = {"tenant_id", "main_category_id"}
                )
        },
        indexes = {
                @Index(name = "idx_tenant_main_categories_tenant", columnList = "tenant_id"),
                @Index(name = "idx_tenant_main_categories_category", columnList = "main_category_id"),
                @Index(name = "idx_tenant_main_categories_active", columnList = "is_active")
        }
)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class TenantMainCategory extends BaseTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    /**
     * Reference to global MainCategory.
     *
     * NOTE: ManyToOne to SHARED entity (no cascade)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_category_id", nullable = false)
    private MainCategory mainCategory;

    /**
     * Tenant-specific budget allocation (percentage).
     *
     * Example: 25.5 means 25.5% of total budget
     */
    @Column(name = "budget_percentage", precision = 5, scale = 2)
    private BigDecimal budgetPercentage;

    /**
     * Tenant-specific accounting code.
     *
     * Example: "600.01", "ACC-CREW-2024"
     */
    @Column(name = "accounting_code", length = 50)
    private String accountingCode;

    /**
     * Tenant can disable a selected category.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Optional tenant-specific notes.
     */
    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected TenantMainCategory() {}

    // === FACTORY METHOD ===

    public static TenantMainCategory create(MainCategory mainCategory) {
        TenantMainCategory tenantCategory = new TenantMainCategory();
        tenantCategory.mainCategory = Objects.requireNonNull(mainCategory, "MainCategory cannot be null");
        tenantCategory.isActive = true;
        tenantCategory.createdAt = LocalDateTime.now();
        tenantCategory.updatedAt = LocalDateTime.now();

        return tenantCategory;
    }

    // === BUSINESS METHODS ===

    public void updateBudget(BigDecimal budgetPercentage) {
        if (budgetPercentage != null) {
            if (budgetPercentage.compareTo(BigDecimal.ZERO) < 0 ||
                    budgetPercentage.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Budget percentage must be between 0 and 100");
            }
        }
        this.budgetPercentage = budgetPercentage;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateAccountingCode(String accountingCode) {
        this.accountingCode = accountingCode;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateNotes(String notes) {
        this.notes = notes;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {  // ✅ activate (fiil)
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {  // ✅ deactivate (fiil)
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    // === GETTERS ===

    public UUID getId() { return id; }
    public MainCategory getMainCategory() { return mainCategory; }
    public BigDecimal getBudgetPercentage() { return budgetPercentage; }
    public String getAccountingCode() { return accountingCode; }
    public Boolean getActive() { return isActive; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TenantMainCategory that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
                "TenantMainCategory{id=%s, tenantId=%s, categoryCode='%s', budget=%s%%}",
                id, getTenantId(),
                mainCategory != null ? mainCategory.getCode() : "null",
                budgetPercentage
        );
    }
}