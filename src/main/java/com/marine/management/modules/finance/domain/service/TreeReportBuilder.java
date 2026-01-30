package com.marine.management.modules.finance.domain.service;

import com.marine.management.modules.finance.application.dto.TreeNodeDTO;
import com.marine.management.modules.finance.domain.model.TreeReportProjection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TreeReportBuilder {

    public List<TreeNodeDTO> buildTree(List<TreeReportProjection> projections) {
        if (projections.isEmpty()) {
            return Collections.emptyList();
        }

        // Calculate total
        BigDecimal total = projections.stream()
                .map(TreeReportProjection::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Group by MainCategory
        Map<Long, List<TreeReportProjection>> mainCategoryGroups = projections.stream()
                .collect(Collectors.groupingBy(
                        p -> p.mainCategoryId() != null ? p.mainCategoryId() : -1L
                ));

        List<TreeNodeDTO> mainCategoryNodes = new ArrayList<>();

        mainCategoryGroups.forEach((mainCatId, mainCatProjections) -> {
            BigDecimal mainCatTotal = mainCatProjections.stream()
                    .map(TreeReportProjection::totalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Build category children
            List<TreeNodeDTO> categoryChildren = buildCategoryLevel(
                    mainCatProjections,
                    mainCatTotal
            );

            TreeNodeDTO mainCatNode;
            if (mainCatId == -1L) {
                mainCatNode = new TreeNodeDTO(
                        1,
                        "MAIN_CATEGORY",
                        "-1",
                        "Unassigned",
                        "Unassigned",
                        mainCatTotal,
                        calculatePercentage(mainCatTotal, total),
                        null,
                        categoryChildren.size(),
                        categoryChildren
                );
            } else {
                var first = mainCatProjections.get(0);
                mainCatNode = new TreeNodeDTO(
                        1,
                        "MAIN_CATEGORY",
                        mainCatId.toString(),
                        first.mainCategoryNameTr(),
                        first.mainCategoryNameEn(),
                        mainCatTotal,
                        calculatePercentage(mainCatTotal, total),
                        first.mainCategoryTechnical(),
                        categoryChildren.size(),
                        categoryChildren
                );
            }

            mainCategoryNodes.add(mainCatNode);
        });

        // Sort by amount descending
        mainCategoryNodes.sort(Comparator.comparing(TreeNodeDTO::amount).reversed());

        return mainCategoryNodes;
    }

    private List<TreeNodeDTO> buildCategoryLevel(
            List<TreeReportProjection> projections,
            BigDecimal parentTotal
    ) {
        Map<UUID, List<TreeReportProjection>> categoryGroups = projections.stream()
                .collect(Collectors.groupingBy(TreeReportProjection::categoryId));

        List<TreeNodeDTO> categoryNodes = new ArrayList<>();

        categoryGroups.forEach((categoryId, catProjections) -> {
            BigDecimal catTotal = catProjections.stream()
                    .map(TreeReportProjection::totalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Build who children
            List<TreeNodeDTO> whoChildren = buildWhoLevel(
                    catProjections,
                    catTotal
            );

            var first = catProjections.get(0);
            TreeNodeDTO categoryNode = new TreeNodeDTO(
                    2,
                    "CATEGORY",
                    categoryId.toString(),
                    first.categoryName(),
                    first.categoryName(),
                    catTotal,
                    calculatePercentage(catTotal, parentTotal),
                    first.categoryTechnical(),
                    whoChildren.size(),
                    whoChildren
            );

            categoryNodes.add(categoryNode);
        });

        categoryNodes.sort(Comparator.comparing(TreeNodeDTO::amount).reversed());

        return categoryNodes;
    }

    private List<TreeNodeDTO> buildWhoLevel(
            List<TreeReportProjection> projections,
            BigDecimal parentTotal
    ) {
        Map<Long, List<TreeReportProjection>> whoGroups = projections.stream()
                .collect(Collectors.groupingBy(
                        p -> p.whoId() != null ? p.whoId() : -1L
                ));

        List<TreeNodeDTO> whoNodes = new ArrayList<>();

        whoGroups.forEach((whoId, whoProjections) -> {
            BigDecimal whoTotal = whoProjections.stream()
                    .map(TreeReportProjection::totalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            TreeNodeDTO whoNode;
            if (whoId == -1L) {
                whoNode = new TreeNodeDTO(
                        3,
                        "WHO",
                        "-1",
                        "Unspecified",
                        "Unspecified",
                        whoTotal,
                        calculatePercentage(whoTotal, parentTotal),
                        null,
                        0,
                        Collections.emptyList()
                );
            } else {
                var first = whoProjections.get(0);
                whoNode = new TreeNodeDTO(
                        3,
                        "WHO",
                        whoId.toString(),
                        first.whoNameTr(),
                        first.whoNameEn(),
                        whoTotal,
                        calculatePercentage(whoTotal, parentTotal),
                        first.whoTechnical(),
                        0,
                        Collections.emptyList()
                );
            }

            whoNodes.add(whoNode);
        });

        whoNodes.sort(Comparator.comparing(TreeNodeDTO::amount).reversed());

        return whoNodes;
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