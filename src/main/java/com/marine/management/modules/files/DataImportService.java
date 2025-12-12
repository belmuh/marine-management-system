package com.marine.management.modules.files;

import com.marine.management.modules.finance.domain.entity.FinancialCategory;
import com.marine.management.modules.finance.domain.entity.FinancialEntry;
import com.marine.management.modules.finance.domain.enums.PaymentMethod;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.vo.EntryNumber;
import com.marine.management.modules.finance.domain.vo.Money;
import com.marine.management.modules.finance.infrastructure.FinancialCategoryRepository;
import com.marine.management.modules.finance.infrastructure.FinancialEntryRepository;
import com.marine.management.modules.users.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class DataImportService {

    private final ExcelParserService excelParserService;
    private final FinancialCategoryRepository categoryRepository;
    private final FinancialEntryRepository entryRepository;
    private final CategoryTypeDeterminer categoryTypeDeterminer;

    public DataImportService(
            ExcelParserService excelParserService,
            FinancialCategoryRepository categoryRepository,
            FinancialEntryRepository entryRepository
    ) {
        this.excelParserService = excelParserService;
        this.categoryRepository = categoryRepository;
        this.entryRepository = entryRepository;
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

        return resultBuilder.build();
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
        Map<String, FinancialCategory> categoryMap = categoryProcessor.processCategories(
                categoryTypeMap,
                resultBuilder
        );

        return categoryMap;
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
        int currentSequence = entryRepository.getNextSequence();
        EntryCreator entryCreator = new EntryCreator(categoryMap, currentUser, currentSequence);

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

    /**
     * Determines category types based on Excel data
     */
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

    /**
     * Helper class to count category usage
     */
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

    /**
     * Creates or finds FinancialCategory entities
     */
    private static class CategoryProcessor {

        private final FinancialCategoryRepository categoryRepository;
        private final CategoryCodeGenerator codeGenerator;

        public CategoryProcessor(FinancialCategoryRepository categoryRepository) {
            this.categoryRepository = categoryRepository;
            this.codeGenerator = new CategoryCodeGenerator();
        }

        public Map<String, FinancialCategory> processCategories(
                Map<String, RecordType> categoryTypeMap,
                ImportResultDto.Builder resultBuilder
        ) {
            Map<String, FinancialCategory> categoryMap = new HashMap<>();
            int createdCount = 0;

            for (Map.Entry<String, RecordType> entry : categoryTypeMap.entrySet()) {
                String categoryName = entry.getKey();
                RecordType categoryType = entry.getValue();
                String categoryCode = codeGenerator.generate(categoryName);

                FinancialCategory category = findOrCreateCategory(
                        categoryName, categoryCode, categoryType, resultBuilder
                );

                categoryMap.put(categoryName, category);


            }

            resultBuilder.categoriesCreated(createdCount);
            return categoryMap;
        }

        private FinancialCategory findOrCreateCategory(
                String categoryName,
                String categoryCode,
                RecordType categoryType,
                ImportResultDto.Builder resultBuilder
        ) {
            return categoryRepository.findByCode(categoryCode)
                    .map(existingCategory -> validateExistingCategory(
                            existingCategory, categoryName, categoryType, resultBuilder
                    ))
                    .orElseGet(() -> createNewCategory(
                            categoryName, categoryCode, categoryType
                    ));
        }

        private FinancialCategory validateExistingCategory(
                FinancialCategory existingCategory,
                String categoryName,
                RecordType suggestedType,
                ImportResultDto.Builder resultBuilder
        ) {
            if (existingCategory.getCategoryType() != suggestedType) {
                logCategoryTypeMismatch(
                        categoryName,
                        existingCategory.getCategoryType(),
                        suggestedType,
                        resultBuilder
                );
            }
            return existingCategory;
        }

        private FinancialCategory createNewCategory(
                String categoryName,
                String categoryCode,
                RecordType categoryType
        ) {
            try {
                boolean isTechnical = determineIfTechnical(categoryName);

                FinancialCategory newCategory = FinancialCategory.create(
                        categoryCode,
                        categoryName,
                        categoryType,
                        "Imported from Excel",
                        getNextDisplayOrder(),
                        isTechnical
                );


                return categoryRepository.save(newCategory);

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

        private void logCategoryTypeMismatch(
                String categoryName,
                RecordType existingType,
                RecordType suggestedType,
                ImportResultDto.Builder resultBuilder
        ) {
            String message = String.format(
                    "Category '%s' has type %s but Excel data suggests %s",
                    categoryName, existingType, suggestedType
            );
            System.out.println(message);
        }
    }

    /**
     * Generates category codes
     */
    private static class CategoryCodeGenerator {

        public String generate(String categoryName) {
            return normalize(categoryName)
                    .toUpperCase()
                    .replaceAll("_+", "_")
                    .replaceAll("^_|_$", "");
        }

        private String normalize(String input) {
            return input.replaceAll("[^A-Za-z0-9]", "_");
        }
    }

    /**
     * Creates FinancialEntry entities
     */
    private static class EntryCreator {

        private final Map<String, FinancialCategory> categoryMap;
        private final User creator;
        private final int startingSequence;
        private int currentSequence;

        public EntryCreator(
                Map<String, FinancialCategory> categoryMap,
                User creator,
                int startingSequence
        ) {
            this.categoryMap = categoryMap;
            this.creator = creator;
            this.startingSequence = startingSequence;
            this.currentSequence = startingSequence;
        }

        public FinancialEntry createEntry(ExcelRow row) {
            FinancialCategory category = getCategoryForRow(row);
            validateCategoryType(row, category);

            Money amount = createMoney(row);
            EntryNumber entryNumber = generateEntryNumber();

            return createFinancialEntry(row, category, amount, entryNumber);
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
            return Money.of(row.amount().toString(), row.currency());
        }

        private EntryNumber generateEntryNumber() {
            return EntryNumber.generate(currentSequence++);
        }

        private FinancialEntry createFinancialEntry(
                ExcelRow row,
                FinancialCategory category,
                Money amount,
                EntryNumber entryNumber
        ) {
            RecordType entryType = row.isIncome() ? RecordType.INCOME : RecordType.EXPENSE;

            // Use appropriate factory method based on entry type

                return FinancialEntry.create(
                        entryNumber,
                        entryType,
                        category,
                        amount,
                        row.date(),
                        determinePaymentMethod(row),
                        row.description(),
                        creator,
                        null,
                        null,
                        null,
                        "Türkiye",
                        null,
                        null,
                        determineIncomeSource(row)
                );

        }

        private PaymentMethod determinePaymentMethod(ExcelRow row) {
            // Default for Excel imports
            return PaymentMethod.CASH;
        }

        private String determineIncomeSource(ExcelRow row) {
            // Extract source from description or use default
            return row.description().contains("Charter") ? "Charter" : "Other";
        }

        private String determineRecipient(ExcelRow row) {
            // Extract recipient logic based on category or description
            return row.category().toLowerCase().contains("salary") ? "Crew" : "Main Yacht";
        }

        private String determineVendor(ExcelRow row) {
            // Extract vendor from description or use category
            return row.description().split(" ")[0]; // Simple extraction
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