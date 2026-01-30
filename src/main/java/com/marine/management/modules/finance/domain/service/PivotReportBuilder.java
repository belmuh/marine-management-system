package com.marine.management.modules.finance.domain.service;

import com.marine.management.modules.finance.application.dto.*;
import com.marine.management.modules.finance.domain.model.PivotReportProjection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class PivotReportBuilder {

    public PivotTreeReportResponse buildPivotReport(
            int year,
            String currency,
            List<PivotReportProjection> projections
    ) {
        // Generate column headers (months)
        List<String> columns = IntStream.rangeClosed(1, 12)
                .mapToObj(m -> String.format("%d-%02d", year, m))
                .toList();

        // Calculate column totals
        Map<String, BigDecimal> columnTotals = calculateColumnTotals(projections, year);

        // Build hierarchical tree with monthly values
        List<PivotTreeNodeDTO> rows = buildPivotTree(projections, year);

        return new PivotTreeReportResponse(
                year,
                currency,
                columns,
                columnTotals,
                rows
        );
    }

    private Map<String, BigDecimal> calculateColumnTotals(List<PivotReportProjection> projections, int year) {
        return projections.stream()
                .collect(Collectors.groupingBy(
                        p -> String.format("%d-%02d", year, p.month()),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                PivotReportProjection::totalAmount,
                                BigDecimal::add
                        )
                ));
    }

    private List<PivotTreeNodeDTO> buildPivotTree(List<PivotReportProjection> projections, int year) {
        // Group by MainCategory
        Map<Long, List<PivotReportProjection>> mainCategoryGroups = projections.stream()
                .collect(Collectors.groupingBy(
                        p -> p.mainCategoryId() != null ? p.mainCategoryId() : -1L
                ));

        List<PivotTreeNodeDTO> mainCategoryNodes = new ArrayList<>();

        mainCategoryGroups.forEach((mainCatId, mainCatProjections) -> {
            Map<String, BigDecimal> monthlyValues = buildMonthlyValues(mainCatProjections, year);

            List<PivotTreeNodeDTO> categoryChildren = buildCategoryLevel(mainCatProjections, year);

            PivotTreeNodeDTO mainCatNode;
            if (mainCatId == -1L) {
                mainCatNode = new PivotTreeNodeDTO(
                        "-1",
                        1,
                        "MAIN_CATEGORY",
                        "Unassigned",
                        "Unassigned",
                        null,
                        monthlyValues,
                        categoryChildren
                );
            } else {
                var first = mainCatProjections.get(0);
                mainCatNode = new PivotTreeNodeDTO(
                        mainCatId.toString(),
                        1,
                        "MAIN_CATEGORY",
                        first.mainCategoryNameTr(),
                        first.mainCategoryNameEn(),
                        first.mainCategoryTechnical(),
                        monthlyValues,
                        categoryChildren
                );
            }

            mainCategoryNodes.add(mainCatNode);
        });

        return mainCategoryNodes;
    }

    private List<PivotTreeNodeDTO> buildCategoryLevel(List<PivotReportProjection> projections, int year) {
        Map<UUID, List<PivotReportProjection>> categoryGroups = projections.stream()
                .collect(Collectors.groupingBy(PivotReportProjection::categoryId));

        List<PivotTreeNodeDTO> categoryNodes = new ArrayList<>();

        categoryGroups.forEach((categoryId, catProjections) -> {
            Map<String, BigDecimal> monthlyValues = buildMonthlyValues(catProjections, year);

            List<PivotTreeNodeDTO> whoChildren = buildWhoLevel(catProjections, year);

            var first = catProjections.get(0);
            PivotTreeNodeDTO categoryNode = new PivotTreeNodeDTO(
                    categoryId.toString(),
                    2,
                    "CATEGORY",
                    first.categoryName(),
                    first.categoryName(),
                    first.categoryTechnical(),
                    monthlyValues,
                    whoChildren
            );

            categoryNodes.add(categoryNode);
        });

        return categoryNodes;
    }

    private List<PivotTreeNodeDTO> buildWhoLevel(List<PivotReportProjection> projections, int year) {
        Map<Long, List<PivotReportProjection>> whoGroups = projections.stream()
                .collect(Collectors.groupingBy(
                        p -> p.whoId() != null ? p.whoId() : -1L
                ));

        List<PivotTreeNodeDTO> whoNodes = new ArrayList<>();

        whoGroups.forEach((whoId, whoProjections) -> {
            Map<String, BigDecimal> monthlyValues = buildMonthlyValues(whoProjections, year);

            PivotTreeNodeDTO whoNode;
            if (whoId == -1L) {
                whoNode = new PivotTreeNodeDTO(
                        "-1",
                        3,
                        "WHO",
                        "Unspecified",
                        "Unspecified",
                        null,
                        monthlyValues,
                        Collections.emptyList()
                );
            } else {
                var first = whoProjections.get(0);
                whoNode = new PivotTreeNodeDTO(
                        whoId.toString(),
                        3,
                        "WHO",
                        first.whoNameTr(),
                        first.whoNameEn(),
                        first.whoTechnical(),
                        monthlyValues,
                        Collections.emptyList()
                );
            }

            whoNodes.add(whoNode);
        });

        return whoNodes;
    }

    private Map<String, BigDecimal> buildMonthlyValues(List<PivotReportProjection> projections, int year) {
        return projections.stream()
                .collect(Collectors.groupingBy(
                        p -> String.format("%d-%02d", year, p.month()),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                PivotReportProjection::totalAmount,
                                BigDecimal::add
                        )
                ));
    }
}