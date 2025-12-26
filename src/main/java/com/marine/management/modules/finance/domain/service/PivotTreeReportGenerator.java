package com.marine.management.modules.finance.domain.service;

import com.marine.management.modules.finance.domain.entity.FinancialEntry;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.model.PivotTreeNode;
import com.marine.management.modules.finance.domain.model.PivotTreeReport;
import com.marine.management.modules.finance.domain.entity.MainCategory;
import com.marine.management.modules.finance.domain.entity.Who;
import com.marine.management.modules.finance.infrastructure.MainCategoryRepository;
import com.marine.management.modules.finance.infrastructure.WhoRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
        import java.util.stream.Collectors;

@Component
public class PivotTreeReportGenerator {

    private static final Long NULL_ID = -1L;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final MainCategoryRepository mainCategoryRepository;
    private final WhoRepository whoRepository;

    public PivotTreeReportGenerator(
            MainCategoryRepository mainCategoryRepository,
            WhoRepository whoRepository
    ) {
        this.mainCategoryRepository = mainCategoryRepository;
        this.whoRepository = whoRepository;
    }

    public PivotTreeReport generatePivotReport(
            List<FinancialEntry> allEntries,
            int year,
            String currency
    ) {
        // Filter expense entries for the year
        List<FinancialEntry> yearEntries = allEntries.stream()
                .filter(entry -> entry.getEntryType() == RecordType.EXPENSE)
                .filter(entry -> entry.getEntryDate().getYear() == year)
                .toList();

        // Load reference data
        Map<Long, MainCategory> mainCategoryMap = loadMainCategories(yearEntries);
        Map<Long, Who> whoMap = loadWhoEntities(yearEntries);

        // Generate columns (months)
        List<String> columns = generateColumns(year);

        // Calculate column totals
        Map<String, BigDecimal> columnTotals = calculateColumnTotals(yearEntries, columns);

        // Build pivot tree
        List<PivotTreeNode> rows = buildPivotTree(
                yearEntries,
                columns,
                mainCategoryMap,
                whoMap
        );

        return new PivotTreeReport(year, currency, columns, columnTotals, rows);
    }

    private List<String> generateColumns(int year) {
        List<String> columns = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            columns.add(String.format("%d-%02d", year, month));
        }
        columns.add("TOTAL");
        return columns;
    }

    private Map<String, BigDecimal> calculateColumnTotals(
            List<FinancialEntry> entries,
            List<String> columns
    ) {
        Map<String, BigDecimal> totals = new HashMap<>();

        // Initialize all columns with zero
        columns.forEach(col -> totals.put(col, BigDecimal.ZERO));

        // Group by month and sum
        entries.forEach(entry -> {
            String monthKey = entry.getEntryDate().format(MONTH_FORMATTER);
            BigDecimal amount = entry.getBaseAmount().getAmount();

            totals.merge(monthKey, amount, BigDecimal::add);
            totals.merge("TOTAL", amount, BigDecimal::add);
        });

        return totals;
    }

    private List<PivotTreeNode> buildPivotTree(
            List<FinancialEntry> entries,
            List<String> columns,
            Map<Long, MainCategory> mainCategoryMap,
            Map<Long, Who> whoMap
    ) {
        // Group by MainCategory
        Map<Long, List<FinancialEntry>> mainCategoryGroups = entries.stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getMainCategoryId() != null
                                ? entry.getMainCategoryId()
                                : NULL_ID
                ));

        List<PivotTreeNode> nodes = new ArrayList<>();

        mainCategoryGroups.forEach((mainCategoryId, mainCategoryEntries) -> {
            MainCategory mainCategory = mainCategoryId.equals(NULL_ID)
                    ? null
                    : mainCategoryMap.get(mainCategoryId);

            // Calculate monthly values for MainCategory
            Map<String, BigDecimal> monthlyValues = calculateMonthlyValues(
                    mainCategoryEntries,
                    columns
            );

            String mainCategoryIdStr = mainCategoryId.toString();

            // Build category children
            List<PivotTreeNode> categoryChildren = buildCategoryNodes(
                    mainCategoryEntries,
                    columns,
                    whoMap,
                    mainCategoryIdStr
            );

            String name = mainCategory != null ? mainCategory.getNameTr() : "Unassigned";
            String nameEn = mainCategory != null ? mainCategory.getNameEn() : "Unassigned";
            Boolean isTechnical = mainCategory != null ? mainCategory.getTechnical() : null;

            PivotTreeNode node = new PivotTreeNode(
                    mainCategoryIdStr,
                    1,
                    "MAIN_CATEGORY",
                    name,
                    nameEn,
                    isTechnical,
                    monthlyValues,
                    categoryChildren
            );

            nodes.add(node);
        });

        // Sort by total descending
        nodes.sort(Comparator.comparing(
                (PivotTreeNode n) -> n.getMonthlyValues().get("TOTAL")
        ).reversed());

        return nodes;
    }

    private List<PivotTreeNode> buildCategoryNodes(
            List<FinancialEntry> entries,
            List<String> columns,
            Map<Long, Who> whoMap,
            String parentId
    ) {
        Map<UUID, List<FinancialEntry>> categoryGroups = entries.stream()
                .collect(Collectors.groupingBy(entry -> entry.getCategory().getId()));

        List<PivotTreeNode> nodes = new ArrayList<>();

        categoryGroups.forEach((categoryUUID, categoryEntries) -> {
            var category = categoryEntries.get(0).getCategory();

            String compositeId = parentId + "-" + categoryUUID;

            Map<String, BigDecimal> monthlyValues = calculateMonthlyValues(
                    categoryEntries,
                    columns
            );

            List<PivotTreeNode> whoChildren = buildWhoNodes(
                    categoryEntries,
                    columns,
                    whoMap,
                    compositeId
            );

            PivotTreeNode node = new PivotTreeNode(
                    compositeId,
                    2,
                    "CATEGORY",
                    category.getName(),
                    category.getName(),
                    category.isTechnical(),
                    monthlyValues,
                    whoChildren
            );

            nodes.add(node);
        });

        nodes.sort(Comparator.comparing(
                (PivotTreeNode n) -> n.getMonthlyValues().get("TOTAL")
        ).reversed());

        return nodes;
    }

    private List<PivotTreeNode> buildWhoNodes(
            List<FinancialEntry> entries,
            List<String> columns,
            Map<Long, Who> whoMap,
            String parentId
    ) {
        Map<Long, List<FinancialEntry>> whoGroups = entries.stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getWhoId() != null ? entry.getWhoId() : NULL_ID
                ));

        List<PivotTreeNode> nodes = new ArrayList<>();

        whoGroups.forEach((whoId, whoEntries) -> {
            Who who = whoId.equals(NULL_ID) ? null : whoMap.get(whoId);

            String compositeId = parentId + "-" + whoId;

            Map<String, BigDecimal> monthlyValues = calculateMonthlyValues(
                    whoEntries,
                    columns
            );

            String name = who != null ? who.getNameTr() : "Unspecified";
            String nameEn = who != null ? who.getNameEn() : "Unspecified";
            Boolean isTechnical = who != null ? who.getTechnical() : null;

            PivotTreeNode node = new PivotTreeNode(
                    compositeId,
                    3,
                    "WHO",
                    name,
                    nameEn,
                    isTechnical,
                    monthlyValues,
                    new ArrayList<>()  // Leaf node
            );

            nodes.add(node);
        });

        nodes.sort(Comparator.comparing(
                (PivotTreeNode n) -> n.getMonthlyValues().get("TOTAL")
        ).reversed());

        return nodes;
    }

    private Map<String, BigDecimal> calculateMonthlyValues(
            List<FinancialEntry> entries,
            List<String> columns
    ) {
        Map<String, BigDecimal> values = new HashMap<>();

        // Initialize with zeros
        columns.forEach(col -> values.put(col, BigDecimal.ZERO));

        // Group by month and sum
        entries.forEach(entry -> {
            String monthKey = entry.getEntryDate().format(MONTH_FORMATTER);
            BigDecimal amount = entry.getBaseAmount().getAmount();

            values.merge(monthKey, amount, BigDecimal::add);
            values.merge("TOTAL", amount, BigDecimal::add);
        });

        return values;
    }

    private Map<Long, MainCategory> loadMainCategories(List<FinancialEntry> entries) {
        Set<Long> ids = entries.stream()
                .map(FinancialEntry::getMainCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (ids.isEmpty()) return Collections.emptyMap();

        return mainCategoryRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(MainCategory::getId, mc -> mc));
    }

    private Map<Long, Who> loadWhoEntities(List<FinancialEntry> entries) {
        Set<Long> ids = entries.stream()
                .map(FinancialEntry::getWhoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (ids.isEmpty()) return Collections.emptyMap();

        return whoRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Who::getId, w -> w));
    }
}