package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.domain.entities.FinancialCategory;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.infrastructure.FinancialCategoryRepository;
import com.marine.management.modules.finance.infrastructure.MainCategoryRepository;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.exceptions.CategoryNotFoundException;
import com.marine.management.shared.multitenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Financial category application service.
 *
 * TENANT ISOLATION:
 * - All queries automatically tenant-filtered (Hibernate @Filter)
 * - TenantContext.getCurrentTenantId() for explicit checks
 * - No manual tenant parameters needed
 * - BaseTenantEntity auto-injects tenant_id on save
 *
 * UNIQUE CONSTRAINTS:
 * - Name uniqueness is per-tenant (tenant_id, name) UNIQUE
 * - Category lookup is tenant-scoped automatically
 *
 * @see FinancialCategory
 * @see TenantContext
 */
@Service
@Transactional(readOnly = true)
public class FinancialCategoryService {

    private static final Logger logger = LoggerFactory.getLogger(FinancialCategoryService.class);

    private final FinancialCategoryRepository categoryRepository;
    private final MainCategoryRepository mainCategoryRepository;

    public FinancialCategoryService(
            FinancialCategoryRepository categoryRepository,
            MainCategoryRepository mainCategoryRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.mainCategoryRepository = mainCategoryRepository;
    }

    // ============================================
    // COMMAND METHODS (Transactional)
    // ============================================

    /**
     * Creates a new financial category.
     *
     * TENANT ISOLATION:
     * - tenant_id automatically injected by TenantEntityListener
     * - Code uniqueness checked within current tenant only
     * - Hibernate filter ensures existsByName is tenant-scoped
     */
    @Transactional
    public FinancialCategory create(
            String name,
            RecordType categoryType,
            String description,
            Integer displayOrder,
            Boolean isTechnical
    ) {
        guardTenantContext();

        logger.debug("Creating category for tenant: {}", TenantContext.getCurrentTenantId());

        // Check name uniqueness within current tenant
        if (categoryRepository.existsByName(name.trim())) {
            throw new IllegalArgumentException(
                    "Category name already exists in your organization: " + name
            );
        }

        // A category may not reuse a main category's name (TR or EN) — that produces
        // a confusing pivot/tree level that looks like the main category duplicated.
        guardNotMainCategoryName(name);

        // Create category (tenant_id auto-injected)
        FinancialCategory category = FinancialCategory.create(
                name,
                categoryType,
                description,
                displayOrder,
                isTechnical
        );

        FinancialCategory saved = categoryRepository.save(category);

        logger.info("Category created: id={}, name='{}', tenant={}",
                saved.getId(),
                saved.getName(),
                TenantContext.getCurrentTenantId());

        return saved;
    }

    /**
     * Updates category details.
     *
     * TENANT ISOLATION:
     * - findById automatically filters by tenant_id
     * - Only categories in current tenant can be updated
     */
    @Transactional
    public FinancialCategory update(
            UUID id,
            String name,
            String description,
            RecordType categoryType,
            Boolean isTechnical
    ) {
        guardTenantContext();

        FinancialCategory category = getByIdOrThrow(id);
        category.updateDetails(name, description, categoryType, isTechnical);

        logger.debug("Category updated: id={}, tenant={}",
                id,
                TenantContext.getCurrentTenantId());

        return category;
    }

    @Transactional
    public FinancialCategory updateDisplayOrder(UUID id, Integer displayOrder) {
        guardTenantContext();

        FinancialCategory category = getByIdOrThrow(id);
        category.changeDisplayOrder(displayOrder);

        return category;
    }

    @Transactional
    public FinancialCategory activate(UUID id) {
        guardTenantContext();

        FinancialCategory category = getByIdOrThrow(id);
        category.activate();

        logger.info("Category activated: id={}, name='{}', tenant={}",
                id,
                category.getName(),
                TenantContext.getCurrentTenantId());

        return category;
    }

    @Transactional
    public FinancialCategory deactivate(UUID id) {
        guardTenantContext();

        FinancialCategory category = getByIdOrThrow(id);
        category.deactivate();

        logger.info("Category deactivated: id={}, name='{}', tenant={}",
                id,
                category.getName(),
                TenantContext.getCurrentTenantId());

        return category;
    }

    /**
     * Deletes a category.
     *
     * TENANT ISOLATION:
     * - Only categories in current tenant can be deleted
     * - Active categories cannot be deleted (business rule)
     */
    @Transactional
    public void delete(UUID id, User deletedBy) {
        guardTenantContext();

        FinancialCategory category = getByIdOrThrow(id);

        if (category.isEnabled()) {
            throw new IllegalStateException(
                    "Cannot delete active category. Deactivate first: " + category.getName()
            );
        }

        category.softDelete(deletedBy);
        categoryRepository.save(category);

        logger.info("Category soft-deleted: id={}, name='{}', by={}, tenant={}",
                id,
                category.getName(),
                deletedBy.getUsername(),
                TenantContext.getCurrentTenantId());
    }

    // ============================================
    // QUERY METHODS (Read-only)
    // ============================================

    /**
     * Find category by ID (tenant-filtered).
     *
     * TENANT ISOLATION:
     * - Hibernate @Filter automatically adds WHERE tenant_id = ?
     * - Returns empty if category doesn't belong to current tenant
     */
    public Optional<FinancialCategory> findById(UUID id) {
        guardTenantContext();

        // Auto tenant-filtered
        return categoryRepository.findById(id);
    }

    /**
     * Find all active categories in current tenant.
     *
     * TENANT ISOLATION:
     * - Auto tenant-filtered
     * - Returns only categories for current organization
     */
    public List<FinancialCategory> findAllActive() {
        guardTenantContext();

        // Auto tenant-filtered
        return categoryRepository.findByEnabledTrueOrderByDisplayOrderAsc();
    }

    /**
     * Find all categories in current tenant.
     *
     * TENANT ISOLATION:
     * - Auto tenant-filtered
     * - Includes both active and inactive
     */
    public List<FinancialCategory> findAll() {
        guardTenantContext();

        // Auto tenant-filtered
        return categoryRepository.findAllByOrderByDisplayOrderAsc();
    }

    /**
     * Search categories by keyword in current tenant.
     *
     * TENANT ISOLATION:
     * - Auto tenant-filtered
     * - Searches only within current organization's categories
     */
    public List<FinancialCategory> searchCategories(String keyword) {
        guardTenantContext();

        // Auto tenant-filtered
        return categoryRepository.search(keyword);
    }

    /**
     * Check if name is unique within current tenant.
     *
     * TENANT ISOLATION:
     * - Name uniqueness is per-tenant
     * - Same name can exist in different tenants
     */
    public boolean isNameUnique(String name) {
        guardTenantContext();

        // Auto tenant-filtered
        return !categoryRepository.existsByName(name);
    }

    /**
     * Find categories by type with usage count.
     *
     * TENANT ISOLATION:
     * - Auto tenant-filtered
     * - Usage count only from current tenant's entries
     */
    public List<FinancialCategoryRepository.CategoryWithUsageCount> findByTypeWithUsageCount(
            RecordType categoryType,
            LocalDate oneYearAgo
    ) {
        guardTenantContext();

        // Auto tenant-filtered (both categories and usage count)
        return categoryRepository.findByTypeWithUsageCount(categoryType, oneYearAgo);
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    /**
     * Guards against missing tenant context.
     *
     * CRITICAL: Service methods should not run without tenant context.
     */
    /**
     * Rejects category names that collide with an existing main category name
     * (Turkish or English, case-insensitive). Keeps the pivot/tree hierarchy
     * unambiguous — a category should never look like a clone of its main category.
     */
    private void guardNotMainCategoryName(String name) {
        String candidate = name.trim();
        boolean clashes = mainCategoryRepository.findAll().stream()
                .anyMatch(mc ->
                        candidate.equalsIgnoreCase(mc.getNameTr())
                                || candidate.equalsIgnoreCase(mc.getNameEn()));
        if (clashes) {
            throw new IllegalArgumentException(
                    "Category name conflicts with a main category name: " + name
            );
        }
    }

    private void guardTenantContext() {
        if (!TenantContext.hasTenantContext()) {
            logger.error("CRITICAL: Service method called without tenant context!");
            throw new AccessDeniedException(
                    "No tenant context available. This is a programming error - " +
                            "service should only be called within tenant context."
            );
        }
    }

    /**
     * Finds category or throws exception.
     *
     * TENANT ISOLATION: findById is auto tenant-filtered.
     */
    private FinancialCategory getByIdOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> CategoryNotFoundException.withId(id));
    }
}