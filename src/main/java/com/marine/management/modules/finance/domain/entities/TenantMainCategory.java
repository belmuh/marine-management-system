package com.marine.management.modules.finance.domain.entities;

import com.marine.management.shared.domain.BaseTenantEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

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
                @Index(name = "idx_tenant_main_categories_enabled", columnList = "is_enabled")
        }
)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class TenantMainCategory extends BaseTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_category_id", nullable = false)
    private MainCategory mainCategory;

    @Column(name = "budget_percentage", precision = 5, scale = 2)
    private BigDecimal budgetPercentage;

    @Column(name = "accounting_code", length = 50)
    private String accountingCode;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = true;  //  Field name without 'is' prefix

    @Column(name = "notes", length = 500)
    private String notes;

    protected TenantMainCategory() {}

    public static TenantMainCategory create(MainCategory mainCategory) {
        TenantMainCategory tenantCategory = new TenantMainCategory();
        tenantCategory.mainCategory = Objects.requireNonNull(mainCategory, "MainCategory cannot be null");
        tenantCategory.enabled = true;  //  Consistent field name

        return tenantCategory;
    }

    public void updateBudget(BigDecimal budgetPercentage) {
        if (budgetPercentage != null) {
            if (budgetPercentage.compareTo(BigDecimal.ZERO) < 0 ||
                    budgetPercentage.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Budget percentage must be between 0 and 100");
            }
        }
        this.budgetPercentage = budgetPercentage;
    }

    public void updateAccountingCode(String accountingCode) {
        this.accountingCode = accountingCode;
    }

    public void updateNotes(String notes) {
        this.notes = notes;
    }

    public void enable() {
        this.enabled = true;  //  Consistent field name
    }

    public void disable() {
        this.enabled = false;  //  Consistent field name
    }

    // === GETTERS ===

    public UUID getId() {
        return id;
    }

    public MainCategory getMainCategory() {
        return mainCategory;
    }

    public BigDecimal getBudgetPercentage() {
        return budgetPercentage;
    }

    public String getAccountingCode() {
        return accountingCode;
    }

    public boolean isEnabled() {  //  Getter with 'is' prefix
        return enabled;
    }

    public void setEnabled(boolean enabled) {  //  Setter without 'is' prefix
        this.enabled = enabled;
    }

    public String getNotes() {
        return notes;
    }

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