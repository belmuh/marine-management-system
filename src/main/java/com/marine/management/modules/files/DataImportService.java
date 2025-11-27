package com.marine.management.modules.files;

import com.marine.management.modules.finance.domain.*;
import com.marine.management.modules.finance.domain.model.Money;
import com.marine.management.modules.finance.infrastructure.FinancialCategoryRepository;
import com.marine.management.modules.finance.infrastructure.FinancialEntryRepository;
import com.marine.management.modules.users.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class DataImportService {

    private final ExcelParserService excelParserService;
    private final FinancialCategoryRepository categoryRepository;
    private final FinancialEntryRepository entryRepository;

    public DataImportService(
            ExcelParserService excelParserService,
            FinancialCategoryRepository categoryRepository,
            FinancialEntryRepository entryRepository
    ) {
        this.excelParserService = excelParserService;
        this.categoryRepository = categoryRepository;
        this.entryRepository = entryRepository;
    }

    @Transactional
    public ImportResultDto importFromExcel(MultipartFile file, User currentUser) throws IOException {
        ImportResultDto.Builder resultBuilder = ImportResultDto.builder();

        // 1. Parse Excel
        List<ExcelRow> rows = excelParserService.parseExcel(file);
        resultBuilder.totalRows(rows.size());

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("No valid data found in Excel file");
        }

        // 2. Extract unique categories
        Set<String> uniqueCategories = new HashSet<>();
        for (ExcelRow row : rows) {
            uniqueCategories.add(row.category());
        }

        // 3. Create or find categories
        Map<String, FinancialCategory> categoryMap = createOrFindCategories(
                uniqueCategories,
                resultBuilder
        );

        // 4. Create entries
        int currentSequence = entryRepository.getNextSequence();
        int successCount = 0;

        for (int i = 0; i < rows.size(); i++) {
            ExcelRow row = rows.get(i);
            int rowNumber = i + 1; // Excel row number (1-indexed + header)

            try {
                createEntry(row, categoryMap, currentUser, currentSequence + i);
                successCount++;
            } catch (Exception e) {
                resultBuilder.addError(
                        rowNumber,
                        "entry",
                        e.getMessage()
                );
            }
        }

        resultBuilder
                .successfulRows(successCount)
                .entriesCreated(successCount);

        return resultBuilder.build();
    }

    private Map<String, FinancialCategory> createOrFindCategories(
            Set<String> categoryNames,
            ImportResultDto.Builder resultBuilder
    ) {
        Map<String, FinancialCategory> categoryMap = new HashMap<>();
        int createdCount = 0;

        for (String categoryName : categoryNames) {
            // Check if category already exists
            Optional<FinancialCategory> existingCategory = categoryRepository
                    .findByCode(generateCategoryCode(categoryName));

            if (existingCategory.isPresent()) {
                categoryMap.put(categoryName, existingCategory.get());
            } else {
                // Create new category
                try {
                    FinancialCategory newCategory = FinancialCategory.create(
                            generateCategoryCode(categoryName),
                            categoryName,
                            " ",
                            categoryMap.size() + 1
                    );
                    FinancialCategory saved = categoryRepository.save(newCategory);
                    categoryMap.put(categoryName, saved);
                    createdCount++;
                } catch (Exception e) {
                    System.err.println("Failed to create category: " + categoryName + " - " + e.getMessage());
                }
            }
        }

        resultBuilder.categoriesCreated(createdCount);
        return categoryMap;
    }

    private void createEntry(
            ExcelRow row,
            Map<String, FinancialCategory> categoryMap,
            User creator,
            int sequence
    ) {
        FinancialCategory category = categoryMap.get(row.category());
        if (category == null) {
            throw new IllegalStateException("Category not found: " + row.category());
        }

        // Determine entry type
        EntryType entryType = row.isIncome() ? EntryType.INCOME : EntryType.EXPENSE;

        // Create Money value object
        Money amount = Money.of(row.amount(), row.currency());

        // Generate entry number
        EntryNumber entryNumber = EntryNumber.generate(sequence);

        // Create entry
        FinancialEntry entry = FinancialEntry.create(
                entryNumber,
                entryType,
                category,
                amount,
                row.date(),
                creator,
                row.description()
        );

        entryRepository.save(entry);
    }

    private String generateCategoryCode(String categoryName) {
        return categoryName
                .toUpperCase()
                .replaceAll("[^A-Z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }
}