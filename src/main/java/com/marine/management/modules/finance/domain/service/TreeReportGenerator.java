package com.marine.management.modules.finance.domain.service;

import com.marine.management.modules.finance.domain.entity.FinancialEntry;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.model.ExpenseTreeReport;
import com.marine.management.modules.finance.domain.model.TreeNode;
import com.marine.management.modules.finance.domain.vo.Period;
import com.marine.management.modules.finance.domain.entity.MainCategory;
import com.marine.management.modules.finance.domain.entity.Who;
import com.marine.management.modules.finance.infrastructure.MainCategoryRepository;
import com.marine.management.modules.finance.infrastructure.WhoRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TreeReportGenerator {

    private static final Long NULL_ID = -1L; // Sentinel value for null grouping

    private final MainCategoryRepository mainCategoryRepository;
    private final WhoRepository whoRepository;

    public TreeReportGenerator(
            MainCategoryRepository mainCategoryRepository,
            WhoRepository whoRepository
    ) {
        this.mainCategoryRepository = mainCategoryRepository;
        this.whoRepository = whoRepository;
    }

    public ExpenseTreeReport generateExpenseTree(
            List<FinancialEntry> entries,
            Period period,
            String currency
    ) {
        // Filter entries by period and expense type
        List<FinancialEntry> periodEntries = entries.stream()
                .filter(entry -> entry.getEntryType() == RecordType.EXPENSE)
                .filter(entry -> period.contains(entry.getEntryDate()))
                .toList();

        // Calculate total
        BigDecimal totalAmount = periodEntries.stream()
                .map(entry -> entry.getBaseAmount().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Load all MainCategories and Who entities once
        Map<Long, MainCategory> mainCategoryMap = loadMainCategories(periodEntries);
        Map<Long, Who> whoMap = loadWhoEntities(periodEntries);

        // Build tree nodes
        List<TreeNode> mainCategoryNodes = buildMainCategoryNodes(
                periodEntries,
                totalAmount,
                mainCategoryMap,
                whoMap
        );

        return new ExpenseTreeReport(period, currency, totalAmount, mainCategoryNodes);
    }

    private Map<Long, MainCategory> loadMainCategories(List<FinancialEntry> entries) {
        Set<Long> mainCategoryIds = entries.stream()
                .map(FinancialEntry::getMainCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (mainCategoryIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return mainCategoryRepository.findAllById(mainCategoryIds).stream()
                .collect(Collectors.toMap(MainCategory::getId, mc -> mc));
    }

    private Map<Long, Who> loadWhoEntities(List<FinancialEntry> entries) {
        Set<Long> whoIds = entries.stream()
                .map(FinancialEntry::getWhoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (whoIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return whoRepository.findAllById(whoIds).stream()
                .collect(Collectors.toMap(Who::getId, w -> w));
    }

    private List<TreeNode> buildMainCategoryNodes(
            List<FinancialEntry> entries,
            BigDecimal totalAmount,
            Map<Long, MainCategory> mainCategoryMap,
            Map<Long, Who> whoMap
    ) {
        // Group by MainCategory ID (including null as NULL_ID)
        Map<Long, List<FinancialEntry>> mainCategoryGroups = entries.stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getMainCategoryId() != null
                                ? entry.getMainCategoryId()
                                : NULL_ID
                ));

        List<TreeNode> nodes = new ArrayList<>();

        mainCategoryGroups.forEach((mainCategoryId, mainCategoryEntries) -> {
            BigDecimal mainCategoryAmount = mainCategoryEntries.stream()
                    .map(entry -> entry.getBaseAmount().getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal percentage = calculatePercentage(mainCategoryAmount, totalAmount);

            TreeNode.Builder nodeBuilder = TreeNode.builder()
                    .level(1)
                    .type(TreeNode.NodeType.MAIN_CATEGORY)
                    .amount(mainCategoryAmount)
                    .percentage(percentage);

            // Handle null MainCategory
            if (mainCategoryId.equals(NULL_ID)) {
                List<TreeNode> categoryChildren = buildCategoryNodes(
                        mainCategoryEntries,
                        mainCategoryAmount,
                        whoMap
                );

                TreeNode unassignedNode = nodeBuilder
                        .id(NULL_ID)
                        .name("Unassigned")
                        .nameEn("Unassigned")
                        .isTechnical(null)
                        .children(categoryChildren)
                        .build();

                nodes.add(unassignedNode);
            } else {
                MainCategory mainCategory = mainCategoryMap.get(mainCategoryId);

                // If MainCategory not found in map, create placeholder
                if (mainCategory == null) {
                    List<TreeNode> categoryChildren = buildCategoryNodes(
                            mainCategoryEntries,
                            mainCategoryAmount,
                            whoMap
                    );

                    TreeNode unknownNode = nodeBuilder
                            .id(mainCategoryId)
                            .name("Unknown MainCategory (ID: " + mainCategoryId + ")")
                            .nameEn("Unknown MainCategory (ID: " + mainCategoryId + ")")
                            .isTechnical(null)
                            .children(categoryChildren)
                            .build();

                    nodes.add(unknownNode);
                    return;
                }

                List<TreeNode> categoryChildren = buildCategoryNodes(
                        mainCategoryEntries,
                        mainCategoryAmount,
                        whoMap
                );

                TreeNode mainCategoryNode = nodeBuilder
                        .id(mainCategory.getId())
                        .name(mainCategory.getNameTr())
                        .nameEn(mainCategory.getNameEn())
                        .isTechnical(mainCategory.getTechnical())
                        .children(categoryChildren)
                        .build();

                nodes.add(mainCategoryNode);
            }
        });

        // Sort by amount descending
        nodes.sort(Comparator.comparing(TreeNode::getAmount).reversed());

        return nodes;
    }

    private List<TreeNode> buildCategoryNodes(
            List<FinancialEntry> entries,
            BigDecimal parentAmount,
            Map<Long, Who> whoMap
    ) {
        // Group by Category UUID (tip güvenli)
        Map<UUID, List<FinancialEntry>> categoryGroups = entries.stream()
                .collect(Collectors.groupingBy(entry -> entry.getCategory().getId()));

        List<TreeNode> nodes = new ArrayList<>();

        categoryGroups.forEach((categoryUUID, categoryEntries) -> {
            var firstEntry = categoryEntries.get(0);
            var category = firstEntry.getCategory();

            BigDecimal categoryAmount = categoryEntries.stream()
                    .map(entry -> entry.getBaseAmount().getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal percentage = calculatePercentage(categoryAmount, parentAmount);

            // Build who children
            List<TreeNode> whoChildren = buildWhoNodes(
                    categoryEntries,
                    categoryAmount,
                    whoMap
            );

            TreeNode categoryNode = TreeNode.builder()
                    .level(2)
                    .type(TreeNode.NodeType.CATEGORY)
                    .id(categoryUUID)  // ← UUID direkt, builder string'e çevirir
                    .name(category.getName())
                    .nameEn(category.getName())
                    .amount(categoryAmount)
                    .percentage(percentage)
                    .isTechnical(category.isTechnical())
                    .children(whoChildren)
                    .build();

            nodes.add(categoryNode);
        });

        nodes.sort(Comparator.comparing(TreeNode::getAmount).reversed());
        return nodes;
    }

    private List<TreeNode> buildWhoNodes(
            List<FinancialEntry> entries,
            BigDecimal parentAmount,
            Map<Long, Who> whoMap
    ) {
        // Group by Who ID (including null as NULL_ID)
        Map<Long, List<FinancialEntry>> whoGroups = entries.stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getWhoId() != null
                                ? entry.getWhoId()
                                : NULL_ID
                ));

        List<TreeNode> nodes = new ArrayList<>();

        whoGroups.forEach((whoId, whoEntries) -> {
            BigDecimal whoAmount = whoEntries.stream()
                    .map(entry -> entry.getBaseAmount().getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal percentage = calculatePercentage(whoAmount, parentAmount);

            TreeNode.Builder nodeBuilder = TreeNode.builder()
                    .level(3)
                    .type(TreeNode.NodeType.WHO)
                    .amount(whoAmount)
                    .percentage(percentage)
                    .children(new ArrayList<>()); // Leaf node

            // Handle null Who
            if (whoId.equals(NULL_ID)) {
                TreeNode unspecifiedNode = nodeBuilder
                        .id(NULL_ID)
                        .name("Unspecified")
                        .nameEn("Unspecified")
                        .isTechnical(null)
                        .build();

                nodes.add(unspecifiedNode);
            } else {
                Who who = whoMap.get(whoId);

                // If Who not found in map, create placeholder
                if (who == null) {
                    TreeNode unknownNode = nodeBuilder
                            .id(whoId)
                            .name("Unknown Who (ID: " + whoId + ")")
                            .nameEn("Unknown Who (ID: " + whoId + ")")
                            .isTechnical(null)
                            .build();

                    nodes.add(unknownNode);
                    return;
                }

                TreeNode whoNode = nodeBuilder
                        .id(who.getId())
                        .name(who.getNameTr())
                        .nameEn(who.getNameEn())
                        .isTechnical(who.getTechnical())
                        .build();

                nodes.add(whoNode);
            }
        });

        // Sort by amount descending
        nodes.sort(Comparator.comparing(TreeNode::getAmount).reversed());

        return nodes;
    }

    private BigDecimal calculatePercentage(BigDecimal amount, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return amount
                .divide(total, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}