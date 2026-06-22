package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.application.commands.CreateEntryCommand;
import com.marine.management.modules.finance.domain.entities.FinancialCategory;
import com.marine.management.modules.finance.domain.entities.FinancialEntry;
import com.marine.management.modules.finance.domain.entities.TenantMainCategory;
import com.marine.management.modules.finance.domain.entities.TenantWhoSelection;
import com.marine.management.modules.finance.domain.vo.EntryNumber;
import com.marine.management.modules.finance.infrastructure.FinancialCategoryRepository;
import com.marine.management.modules.finance.infrastructure.TenantEntryCounterRepository;
import com.marine.management.modules.finance.infrastructure.TenantMainCategoryRepository;
import com.marine.management.modules.finance.infrastructure.TenantWhoSelectionRepository;
import com.marine.management.shared.multitenant.TenantContext;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.UUID;
import java.util.Objects;

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

    private final TenantEntryCounterRepository entryCounterRepository;
    private final FinancialCategoryRepository categoryRepository;
    private final TenantWhoSelectionRepository tenantWhoRepository;
    private final TenantMainCategoryRepository tenantMainCategoryRepository;
    private final TenantBaseCurrencyProvider tenantBaseCurrencyProvider;

    public FinancialEntryFactory(
            TenantEntryCounterRepository entryCounterRepository,
            FinancialCategoryRepository categoryRepository,
            TenantWhoSelectionRepository tenantWhoRepository,
            TenantMainCategoryRepository tenantMainCategoryRepository,
            TenantBaseCurrencyProvider tenantBaseCurrencyProvider
    ) {
        this.entryCounterRepository = entryCounterRepository;
        this.categoryRepository = categoryRepository;
        this.tenantWhoRepository = tenantWhoRepository;
        this.tenantMainCategoryRepository = tenantMainCategoryRepository;
        this.tenantBaseCurrencyProvider = Objects.requireNonNull(tenantBaseCurrencyProvider);
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

        // Get the tenant's configured base currency
        String baseCurrency = tenantBaseCurrencyProvider.getCurrentTenantBaseCurrency();

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
                command.vendor(),
                baseCurrency
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
     * Mevcut tenant ve yıl için bir sonraki entry numarasını üretir.
     *
     * TenantContext her authenticated request'te TenantFilter tarafından
     * set edilir — burada null gelmesi mümkün değil.
     */
    private EntryNumber generateEntryNumber() {
        Long tenantId = TenantContext.getCurrentTenantId();
        int year = Year.now().getValue();
        int sequence = entryCounterRepository.nextSequence(tenantId, year);
        return EntryNumber.generate(sequence);
    }
}