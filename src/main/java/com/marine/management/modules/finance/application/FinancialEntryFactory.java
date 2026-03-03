package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.application.commands.CreateEntryCommand;
import com.marine.management.modules.finance.domain.entities.FinancialCategory;
import com.marine.management.modules.finance.domain.entities.FinancialEntry;
import com.marine.management.modules.finance.domain.entities.TenantMainCategory;
import com.marine.management.modules.finance.domain.entities.TenantWhoSelection;
import com.marine.management.modules.finance.domain.vo.EntryNumber;
import com.marine.management.modules.finance.infrastructure.FinancialCategoryRepository;
import com.marine.management.modules.finance.infrastructure.FinancialEntryRepository;
import com.marine.management.modules.finance.infrastructure.TenantMainCategoryRepository;
import com.marine.management.modules.finance.infrastructure.TenantWhoSelectionRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Factory for creating FinancialEntry domain objects.
 *
 * RESPONSIBILITIES:
 * - Resolve entity references (Category, WHO, MainCategory)
 * - Generate entry numbers
 * - Orchestrate entry creation
 *
 * TENANT ISOLATION:
 * - All repository queries are auto tenant-filtered
 * - TenantEntityListener injects tenant_id on save
 */
@Component
public class FinancialEntryFactory {

    private final FinancialEntryRepository entryRepository;
    private final FinancialCategoryRepository categoryRepository;
    private final TenantWhoSelectionRepository tenantWhoRepository;
    private final TenantMainCategoryRepository tenantMainCategoryRepository;

    public FinancialEntryFactory(
            FinancialEntryRepository entryRepository,
            FinancialCategoryRepository categoryRepository,
            TenantWhoSelectionRepository tenantWhoRepository,
            TenantMainCategoryRepository tenantMainCategoryRepository
    ) {
        this.entryRepository = entryRepository;
        this.categoryRepository = categoryRepository;
        this.tenantWhoRepository = tenantWhoRepository;
        this.tenantMainCategoryRepository = tenantMainCategoryRepository;
    }

    /**
     * Creates a new financial entry from command.
     *
     * @param command entry creation parameters
     * @return new FinancialEntry (not yet persisted)
     */
    public FinancialEntry createEntry(CreateEntryCommand command) {
        // Resolve category (auto tenant-filtered)
        FinancialCategory category = categoryRepository.findById(command.categoryId())
                .orElseThrow(() -> new FinancialEntryService.CategoryNotFoundException(
                        "Category not found with id: " + command.categoryId()
                ));

        // Resolve optional references
        TenantWhoSelection tenantWho = resolveTenantWho(command.whoId());
        TenantMainCategory tenantMainCategory = resolveTenantMainCategory(command.mainCategoryId());

        // Generate entry number
        EntryNumber entryNumber = generateEntryNumber();

        // Create domain object (NO tenant parameter - TenantEntityListener handles it)
        return FinancialEntry.create(
                entryNumber,
                command.entryType(),
                category,
                command.amount(),
                command.entryDate(),
                command.paymentMethod(),
                command.description(),
                tenantWho,
                tenantMainCategory,
                command.recipient(),
                command.country(),
                command.city(),
                command.specificLocation(),
                command.vendor()
        );
    }

    /**
     * Resolves TenantWhoSelection from ID.
     *
     * @param whoId WHO selection ID (nullable)
     * @return TenantWhoSelection or null
     */
    private TenantWhoSelection resolveTenantWho(UUID whoId) {
        if (whoId == null) {
            return null;
        }
        return tenantWhoRepository.findById(whoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "TenantWhoSelection not found with id: " + whoId
                ));
    }

    /**
     * Resolves TenantMainCategory from ID.
     *
     * @param mainCategoryId main category ID (nullable)
     * @return TenantMainCategory or null
     */
    private TenantMainCategory resolveTenantMainCategory(UUID mainCategoryId) {
        if (mainCategoryId == null) {
            return null;
        }
        return tenantMainCategoryRepository.findById(mainCategoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "TenantMainCategory not found with id: " + mainCategoryId
                ));
    }

    /**
     * Generates next entry number for current tenant.
     *
     * TENANT ISOLATION: Sequence is tenant-scoped (auto-filtered).
     */
    private EntryNumber generateEntryNumber() {
        int sequence = entryRepository.getNextSequence();
        return EntryNumber.generate(sequence);
    }
}