package com.marine.management.modules.finance.domain.entity;

import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.shared.domain.BaseTenantEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "financial_categories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_financial_categories_tenant_code",
                        columnNames = {"tenant_id", "code"}
                )
        },
        indexes = {
                @Index(name = "idx_financial_categories_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_financial_categories_tenant_code", columnList = "tenant_id, code"),
                @Index(name = "idx_financial_categories_active", columnList = "is_active"),
                @Index(name = "idx_financial_categories_type", columnList = "category_type")
        }
)

@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class FinancialCategory extends BaseTenantEntity {

    private static final String CODE_PATTERN = "^[A-Z0-9_]+$";
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_CODE_LENGTH = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = MAX_CODE_LENGTH, updatable = false)
    private String code;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", nullable = false, length = 20)
    private RecordType categoryType;

    @Column(length = 500)
    private String description;

    @Column(name = "is_technical", nullable = false)
    private boolean isTechnical = true;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected FinancialCategory() {}

    // === FACTORY METHOD ===
    /**
     * Creates a new financial category.
     *
     * CRITICAL: tenant_id is automatically injected by TenantEntityListener.
     * No need to pass tenant as parameter!
     *
     * @param code category code (uppercase, unique per tenant)
     * @param name category name
     * @param categoryType INCOME or EXPENSE
     * @param description optional description
     * @param displayOrder sort order
     * @param isTechnical technical category flag
     * @return new FinancialCategory instance
     */
    public static FinancialCategory create(
            String code,
            String name,
            RecordType categoryType,
            String description,
            Integer displayOrder,
            boolean isTechnical
    ) {
        FinancialCategory category = new FinancialCategory();
        category.code = code.toUpperCase().trim();
        category.name = name.trim();
        category.categoryType = categoryType;
        category.description = description != null ? description.trim() : "";
        category.displayOrder = displayOrder;
        category.isTechnical = isTechnical;
        category.createdAt = LocalDateTime.now();

        category.validate();
        return category;
    }

    // === BUSINESS METHODS ===

    /**
     * Updates category details.
     *
     * NOTE: Code cannot be changed (immutable for data integrity).
     */
    public void updateDetails(String name, String description, RecordType categoryType, Boolean isTechnical) {
        this.name = Objects.requireNonNull(name, "Name cannot be null").trim();
        this.description = description != null ? description.trim() : "";
        this.categoryType = Objects.requireNonNull(categoryType, "Category type cannot be null");
        this.isTechnical = Objects.requireNonNull(isTechnical, "isTechnical cannot be null");

        validate();
    }

    /**
     * Changes the display order for UI sorting.
     */
    public void changeDisplayOrder(Integer newOrder) {
        this.displayOrder = newOrder;
    }

    /**
     * Activates the category (makes it available for use).
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * Deactivates the category (hides from UI, prevents new usage).
     *
     * NOTE: Existing entries with this category remain valid.
     */
    public void deactivate() {
        this.isActive = false;
    }

    // === VALIDATION ===

    /**
     * Validates category business rules.
     *
     * NOTE: tenant_id validation is handled by BaseTenantEntity!
     */
    private void validate() {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalStateException("Category code cannot be empty");
        }
        if (code.length() > MAX_CODE_LENGTH) {
            throw new IllegalStateException(
                    String.format("Category code cannot exceed %d characters", MAX_CODE_LENGTH)
            );
        }
        if (!code.matches(CODE_PATTERN)) {
            throw new IllegalStateException(
                    "Category code must contain only uppercase letters, numbers and underscores"
            );
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalStateException("Category name cannot be empty");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalStateException(
                    String.format("Category name cannot exceed %d characters", MAX_NAME_LENGTH)
            );
        }
        if (categoryType == null) {
            throw new IllegalStateException("Category type is required");
        }
    }

    // === GETTERS ===

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public RecordType getCategoryType() {
        return categoryType;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTechnical() {
        return isTechnical;
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

    // === EQUALS/HASHCODE ===

    /**
     * Equality based on UUID (primary key).
     *
     * WHY UUID (not tenant_id + code)?
     * - UUID is stable (doesn't change after persist)
     * - Safe for collections (HashSet, HashMap)
     * - Generated immediately (not null in transient state)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FinancialCategory cat)) return false;
        return id != null && id.equals(cat.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return String.format(
                "FinancialCategory{id=%s, tenantId=%s, code='%s', name='%s', active=%s}",
                id, getTenantId(), code, name, isActive
        );
    }
}