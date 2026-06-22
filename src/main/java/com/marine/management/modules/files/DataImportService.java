package com.marine.management.modules.files;

import com.marine.management.modules.finance.application.TenantBaseCurrencyProvider;
import com.marine.management.modules.finance.domain.entities.FinancialCategory;
import com.marine.management.modules.finance.domain.entities.FinancialEntry;
import com.marine.management.modules.finance.domain.entities.Payment;
import com.marine.management.modules.finance.domain.enums.PaymentMethod;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.vo.EntryNumber;
import com.marine.management.modules.finance.domain.vo.Money;
import com.marine.management.modules.finance.infrastructure.FinancialCategoryRepository;
import com.marine.management.modules.finance.infrastructure.FinancialEntryRepository;
import com.marine.management.modules.finance.infrastructure.TenantEntryCounterRepository;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.multitenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Imports historical financial data from Excel files.
 *
 * BUSINESS RULES (import = completed historical records):
 * - Imported entries are created as PAID: DRAFT → submitAndApprove() → addPayment(full amount).
 *   Rationale: these are past, settled transactions; leaving them DRAFT would hide them
 *   from financial reports (only ACTUAL statuses are reported) and leaving them APPROVED
 *   would show them as outstanding/unpaid.
 * - Unknown categories are auto-created (type determined by majority usage in the file).
 * - Currency: as provided by the parser (currently EUR-only).
 *
 * TENANT ISOLATION: repositories are auto tenant-filtered; TenantEntityListener
 * injects tenant_id on save. Entry numbers come from the DB sequence per row
 * (NEXTVAL is atomic — see W1 fix notes below in processEntries).
 */
@Service
@Transactional
public class DataImportService {

    private static final Logger log = LoggerFactory.getLogger(DataImportService.class);

    private final ExcelParserService excelParserService;
    private final FinancialCategoryRepository categoryRepository;
    private final FinancialEntryRepository entryRepository;
    private final TenantEntryCounterRepository entryCounterRepository;
    private final TenantBaseCurrencyProvider tenantBaseCurrencyProvider;
    private final CategoryTypeDeterminer categoryTypeDeterminer;

    public DataImportService(
            ExcelParserService excelParserService,
            FinancialCategoryRepository categoryRepository,
            FinancialEntryRepository entryRepository,
            TenantEntryCounterRepository entryCounterRepository,
            TenantBaseCurrencyProvider tenantBaseCurrencyProvider
    ) {
        this.excelParserService = excelParserService;
        this.categoryRepository = categoryRepository;
        this.entryRepository = entryRepository;
        this.entryCounterRepository = entryCounterRepository;
        this.tenantBaseCurrencyProvider = tenantBaseCurrencyProvider;
        this.categoryTypeDeterminer = new CategoryTypeDeterminer();
    }

    public ImportResultDto importFromExcel(MultipartFile file, User currentUser) throws IOException {
        ImportResultDto.Builder resultBuilder = ImportResultDto.builder();

        // 1. Parse and validate Excel data
        List<ExcelRow> excelRows = parseExcelFile(file, resultBuilder);

        // 2. Process categories
        Map<String, FinancialCategory> categoryMap = processCategories(excelRows, resultBuilder);

        // 3. Process entries
        processEntries(excelRows, categoryMap, currentUser, resultBuilder);

        ImportResultDto result = resultBuilder.build();
        log.info("Excel import completed by user {}: {} total rows, {} successful, {} failed, {} categories created",
                currentUser.getId(), result.totalRows(), result.successfulRows(),
                result.failedRows(), result.categoriesCreated());
        return result;
    }

    // ============================================
    // STEP 1: PARSE EXCEL
    // ============================================

    private List<ExcelRow> parseExcelFile(MultipartFile file, ImportResultDto.Builder resultBuilder)
            throws IOException {

        List<ExcelRow> rows = excelParserService.parseExcel(file);
        resultBuilder.totalRows(rows.size());

        if (rows.isEmpty()) {
            throw new EmptyExcelFileException("No valid data found in Excel file");
        }

        return rows;
    }

    // ============================================
    // STEP 2: PROCESS CATEGORIES
    // ============================================

    private Map<String, FinancialCategory> processCategories(
            List<ExcelRow> excelRows,
            ImportResultDto.Builder resultBuilder
    ) {
        // Extract category information from rows
        Map<String, RecordType> categoryTypeMap = categoryTypeDeterminer.determineCategoryTypes(excelRows);

        // Create or find categories
        CategoryProcessor categoryProcessor = new CategoryProcessor(categoryRepository);
        return categoryProcessor.processCategories(categoryTypeMap, resultBuilder);
    }

    // ============================================
    // STEP 3: PROCESS ENTRIES
    // ============================================

    private void processEntries(
            List<ExcelRow> excelRows,
            Map<String, FinancialCategory> categoryMap,
            User currentUser,
            ImportResultDto.Builder resultBuilder
    ) {
        String baseCurrency = tenantBaseCurrencyProvider.getCurrentTenantBaseCurrency();
        Long tenantId = TenantContext.getCurrentTenantId();
        int year = java.time.Year.now().getValue();
        EntryCreator entryCreator = new EntryCreator(
                categoryMap, currentUser,
                () -> entryCounterRepository.nextSequence(tenantId, year),
                baseCurrency);

        int successCount = 0;

        for (int i = 0; i < excelRows.size(); i++) {
            ExcelRow row = excelRows.get(i);
            int rowNumber = i + 2; // Excel row number (1-indexed + header)

            try {
                FinancialEntry entry = entryCreator.createEntry(row);
                entryRepository.save(entry);
                successCount++;
            } catch (ImportException e) {
                resultBuilder.addError(rowNumber, "entry", e.getMessage());
            } catch (Exception e) {
                log.warn("Unexpected error importing row {}", rowNumber, e);
                resultBuilder.addError(rowNumber, "system",
                        "Unexpected error: " + e.getMessage());
            }
        }

        resultBuilder.successfulRows(successCount)
                .entriesCreated(successCount);
    }

    // ============================================
    // INNER CLASSES (Single Responsibility)
    // ============================================

    private static class CategoryTypeDeterminer {

        public Map<String, RecordType> determineCategoryTypes(List<ExcelRow> rows) {
            CategoryUsageCounter usageCounter = countCategoryUsage(rows);
            return assignCategoryTypes(usageCounter);
        }

        private CategoryUsageCounter countCategoryUsage(List<ExcelRow> rows) {
            CategoryUsageCounter counter = new CategoryUsageCounter();

            for (ExcelRow row : rows) {
                String category = row.category();
                if (row.isIncome()) {
                    counter.incrementIncome(category);
                } else {
                    counter.incrementExpense(category);
                }
            }

            return counter;
        }

        private Map<String, RecordType> assignCategoryTypes(CategoryUsageCounter counter) {
            Map<String, RecordType> categoryTypeMap = new HashMap<>();

            for (String category : counter.getAllCategories()) {
                RecordType determinedType = determineTypeByMajority(counter, category);
                categoryTypeMap.put(category, determinedType);
            }

            return categoryTypeMap;
        }

        private RecordType determineTypeByMajority(
                CategoryUsageCounter counter,
                String category
        ) {
            int incomeCount = counter.getIncomeCount(category);
            int expenseCount = counter.getExpenseCount(category);

            return incomeCount > expenseCount ?
                    RecordType.INCOME : RecordType.EXPENSE;
        }
    }

    // Helper class to count category usage

    private static class CategoryUsageCounter {
        private final Map<String, Integer> incomeCount = new HashMap<>();
        private final Map<String, Integer> expenseCount = new HashMap<>();

        public void incrementIncome(String category) {
            incomeCount.merge(category, 1, Integer::sum);
        }

        public void incrementExpense(String category) {
            expenseCount.merge(category, 1, Integer::sum);
        }

        public int getIncomeCount(String category) {
            return incomeCount.getOrDefault(category, 0);
        }

        public int getExpenseCount(String category) {
            return expenseCount.getOrDefault(category, 0);
        }

        public Set<String> getAllCategories() {
            Set<String> allCategories = new HashSet<>();
            allCategories.addAll(incomeCount.keySet());
            allCategories.addAll(expenseCount.keySet());
            return allCategories;
        }
    }

    // Creates or finds FinancialCategory entities

    private static class CategoryProcessor {

        private final FinancialCategoryRepository categoryRepository;
        private int createdCount = 0;

        public CategoryProcessor(FinancialCategoryRepository categoryRepository) {
            this.categoryRepository = categoryRepository;
        }

        public Map<String, FinancialCategory> processCategories(
                Map<String, RecordType> categoryTypeMap,
                ImportResultDto.Builder resultBuilder
        ) {
            Map<String, FinancialCategory> categoryMap = new HashMap<>();

            for (Map.Entry<String, RecordType> entry : categoryTypeMap.entrySet()) {
                String categoryName = entry.getKey();
                RecordType categoryType = entry.getValue();

                FinancialCategory category = findOrCreateCategory(
                        categoryName, categoryType, resultBuilder
                );

                categoryMap.put(categoryName, category);
            }

            resultBuilder.categoriesCreated(createdCount);
            return categoryMap;
        }

        private FinancialCategory findOrCreateCategory(
                String categoryName,
                RecordType categoryType,
                ImportResultDto.Builder resultBuilder
        ) {
            return categoryRepository.findByName(categoryName.trim())
                    .map(existingCategory -> validateExistingCategory(
                            existingCategory, categoryName, categoryType
                    ))
                    .orElseGet(() -> createNewCategory(
                            categoryName, categoryType
                    ));
        }

        private FinancialCategory validateExistingCategory(
                FinancialCategory existingCategory,
                String categoryName,
                RecordType suggestedType
        ) {
            if (existingCategory.getCategoryType() != suggestedType) {
                log.info("Category '{}' has type {} but Excel data suggests {}",
                        categoryName, existingCategory.getCategoryType(), suggestedType);
            }
            return existingCategory;
        }

        private FinancialCategory createNewCategory(
                String categoryName,
                RecordType categoryType
        ) {
            try {
                boolean isTechnical = determineIfTechnical(categoryName);

                FinancialCategory newCategory = FinancialCategory.create(
                        categoryName,
                        categoryType,
                        "Imported from Excel",
                        getNextDisplayOrder(),
                        isTechnical
                );

                FinancialCategory saved = categoryRepository.save(newCategory);
                createdCount++;
                return saved;

            } catch (Exception e) {
                throw new CategoryCreationException(
                        "Failed to create category: " + categoryName, e
                );
            }
        }

        private boolean determineIfTechnical(String categoryName) {
            String name = categoryName.toLowerCase();
            return name.contains("marina") ||
                    name.contains("oil") ||
                    name.contains("fuel") ||
                    name.contains("repair") ||
                    name.contains("maintenance");
        }

        private int getNextDisplayOrder() {
            return (int) categoryRepository.count() + 1;
        }
    }

    // Creates FinancialEntry entities as PAID historical records

    private static class EntryCreator {

        private final Map<String, FinancialCategory> categoryMap;
        private final User creator;
        private final java.util.function.IntSupplier sequenceSupplier;
        private final String baseCurrency;

        public EntryCreator(
                Map<String, FinancialCategory> categoryMap,
                User creator,
                java.util.function.IntSupplier sequenceSupplier,
                String baseCurrency
        ) {
            this.categoryMap = categoryMap;
            this.creator = creator;
            this.sequenceSupplier = sequenceSupplier;
            this.baseCurrency = baseCurrency;
        }

        public FinancialEntry createEntry(ExcelRow row) {
            FinancialCategory category = getCategoryForRow(row);
            validateCategoryType(row, category);

            Money amount = createMoney(row);
            EntryNumber entryNumber = generateEntryNumber();

            FinancialEntry entry = createFinancialEntry(row, category, amount, entryNumber);
            markAsPaidHistoricalRecord(entry, row);
            return entry;
        }

        private FinancialCategory getCategoryForRow(ExcelRow row) {
            FinancialCategory category = categoryMap.get(row.category());
            if (category == null) {
                throw new ImportException("Category not found: " + row.category());
            }
            return category;
        }

        private void validateCategoryType(ExcelRow row, FinancialCategory category) {
            RecordType entryType = row.isIncome() ? RecordType.INCOME : RecordType.EXPENSE;

            if (category.getCategoryType() != entryType) {
                throw new ImportException(String.format(
                        "Category '%s' is type %s but entry is type %s",
                        row.category(), category.getCategoryType(), entryType
                ));
            }
        }

        private Money createMoney(ExcelRow row) {
            return Money.of(row.amount(), row.currency());
        }

        private EntryNumber generateEntryNumber() {
            return EntryNumber.generate(sequenceSupplier.getAsInt());
        }

        private FinancialEntry createFinancialEntry(
                ExcelRow row,
                FinancialCategory category,
                Money amount,
                EntryNumber entryNumber
        ) {
            RecordType entryType = row.isIncome() ? RecordType.INCOME : RecordType.EXPENSE;

            return FinancialEntry.create(
                    entryNumber,
                    entryType,
                    category,
                    amount,
                    row.date(),
                    determinePaymentMethod(row),
                    row.description(),
                    null,           // tenantWho — not available in Excel data
                    null,           // tenantMainCategory — not available in Excel data
                    null,           // recipient
                    null,           // country
                    null,           // city
                    null,           // specificLocation
                    null,           // vendor
                    baseCurrency
            );
        }

        /**
         * Historical records are completed transactions:
         * DRAFT → APPROVED (submitAndApprove) → PAID (full payment dated at entry date).
         */
        private void markAsPaidHistoricalRecord(FinancialEntry entry, ExcelRow row) {
            entry.submitAndApprove();

            Payment payment = Payment.create(
                    entry,
                    entry.getBaseAmount(),
                    row.date(),
                    null,
                    determinePaymentMethod(row),
                    "Imported from Excel",
                    creator
            );
            entry.addPayment(payment);
        }

        private PaymentMethod determinePaymentMethod(ExcelRow row) {
            // Default for Excel imports
            return PaymentMethod.CASH;
        }
    }

    // ============================================
    // CUSTOM EXCEPTIONS
    // ============================================

    public static class EmptyExcelFileException extends RuntimeException {
        public EmptyExcelFileException(String message) {
            super(message);
        }
    }

    public static class ImportException extends RuntimeException {
        public ImportException(String message) {
            super(message);
        }
    }

    public static class CategoryCreationException extends RuntimeException {
        public CategoryCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
