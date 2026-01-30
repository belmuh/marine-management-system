package com.marine.management.modules.finance.application.mapper;

import com.marine.management.modules.finance.application.dto.*;
import com.marine.management.modules.finance.domain.model.*;
import com.marine.management.modules.finance.presentation.dto.reports.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ReportMapper {

    // ============================================
    // ANNUAL / PERIOD
    // ============================================

    public AnnualBreakdownDto toAnnualBreakdownDto(AnnualReport report) {
        return new AnnualBreakdownDto(
                report.getYear(),
                mapCategoryBreakdowns(report.getCategoryBreakdowns()),
                mapMonthlyTotals(report.getMonthlyTotals(), this::toMonthlyTotalDtoFromAnnual),
                report.getGrandTotal(),
                report.getRemainingMoney()
        );
    }

    public PeriodBreakdownDto toPeriodBreakdownDto(PeriodReport report) {
        return new PeriodBreakdownDto(
                report.getPeriod().startDate().toString(),
                report.getPeriod().endDate().toString(),
                convertMonthlyBreakdownsToCategorySummaries(report.getCategoryBreakdowns()),
                mapMonthlyTotals(report.getMonthlyTotals(), this::toMonthlyTotalDtoFromPeriod),
                report.getTotalIncome(),
                report.getTotalExpense(),
                report.getNetBalance()
        );
    }

    // AnnualReport için: CategoryYearSummary → CategoryMonthlyDataDto
    private List<CategoryMonthlyDataDto> mapCategoryBreakdowns(List<CategoryYearSummary> breakdowns) {
        return breakdowns.stream()
                .map(b -> new CategoryMonthlyDataDto(
                        b.categoryName(),
                        b.monthlyValues().entrySet()
                                .stream()
                                .collect(Collectors.toMap(
                                        e -> String.valueOf(e.getKey()),
                                        Map.Entry::getValue
                                )),
                        b.yearTotal()
                ))
                .toList();
    }

    // ✅ PeriodReport için: MonthlyBreakdown → CategoryMonthlyDataDto
    private List<CategoryMonthlyDataDto> convertMonthlyBreakdownsToCategorySummaries(
            List<MonthlyBreakdown> breakdowns) {

        return breakdowns.stream()
                .map(breakdown -> {
                    // String key'li map'e çevir (DTO field: monthlyAmounts)
                    Map<String, BigDecimal> monthlyAmounts = breakdown.monthlyValues().entrySet().stream()
                            .collect(Collectors.toMap(
                                    e -> String.valueOf(e.getKey()),
                                    Map.Entry::getValue
                            ));

                    return new CategoryMonthlyDataDto(
                            breakdown.categoryName(),
                            monthlyAmounts,
                            breakdown.yearTotal()
                    );
                })
                .sorted(Comparator.comparing(CategoryMonthlyDataDto::categoryName))
                .toList();
    }

    private <T> List<MonthlyTotalDto> mapMonthlyTotals(Map<Integer, T> totals,
                                                       BiFunction<Integer, T, MonthlyTotalDto> mapper) {
        return totals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> mapper.apply(e.getKey(), e.getValue()))
                .toList();
    }

    private MonthlyTotalDto toMonthlyTotalDtoFromAnnual(int month, MonthlyTotal total) {
        return new MonthlyTotalDto(
                month,
                total.income(),
                total.expense(),
                total.cumulative()
        );
    }

    private MonthlyTotalDto toMonthlyTotalDtoFromPeriod(int month, MonthlyTotal total) {
        return new MonthlyTotalDto(month, total.income(), total.expense(), total.cumulative());
    }

    // ============================================
    // TREE REPORT
    // ============================================

    public TreeReportResponse toTreeReportResponse(String period, String currency, List<TreeReportProjection> projections) {
        BigDecimal grandTotal = sumTotals(projections, TreeReportProjection::totalAmount);
        List<TreeNodeDTO> nodes = buildTreeNodes(projections, grandTotal);
        return new TreeReportResponse(period, currency, grandTotal, nodes);
    }

    private List<TreeNodeDTO> buildTreeNodes(List<TreeReportProjection> projections, BigDecimal grandTotal) {
        return groupAndMap(projections,
                p -> Optional.ofNullable(p.mainCategoryId()).orElse(-1L),
                (id, group) -> buildTreeNode(id, group, grandTotal, 1),
                Comparator.comparing(TreeNodeDTO::amount).reversed());
    }

    private TreeNodeDTO buildTreeNode(Long id, List<TreeReportProjection> projections, BigDecimal parentTotal, int level) {
        if (projections.isEmpty()) {
            return new TreeNodeDTO(
                    level,
                    level == 1 ? "MAIN_CATEGORY" : level == 2 ? "CATEGORY" : "WHO",
                    level == 3 ? "-1" : "0",
                    "Unassigned",
                    "Unassigned",
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    null,
                    0,
                    Collections.emptyList()
            );
        }

        BigDecimal total = sumTotals(projections, TreeReportProjection::totalAmount);
        List<TreeNodeDTO> children;

        switch (level) {
            case 1 -> children = buildTreeNodesByCategory(projections, total);
            case 2 -> children = buildTreeNodesByWho(projections, total);
            default -> children = Collections.emptyList();
        }

        var first = projections.get(0);

        if (level == 1) { // MainCategory
            return new TreeNodeDTO(
                    level, "MAIN_CATEGORY", id.toString(),
                    id == -1L ? "Unassigned" : first.mainCategoryNameTr(),
                    id == -1L ? "Unassigned" : first.mainCategoryNameEn(),
                    total,
                    calculatePercentage(total, parentTotal),
                    first.mainCategoryTechnical(),
                    children.size(),
                    children
            );
        } else if (level == 2) { // Category
            return new TreeNodeDTO(
                    level, "CATEGORY", first.categoryId().toString(),
                    first.categoryName(),
                    first.categoryName(),
                    total,
                    calculatePercentage(total, parentTotal),
                    first.categoryTechnical(),
                    children.size(),
                    children
            );
        } else { // Who
            return new TreeNodeDTO(
                    level, "WHO", first.whoId() != null ? first.whoId().toString() : "-1",
                    first.whoNameTr() != null ? first.whoNameTr() : "Unspecified",
                    first.whoNameEn() != null ? first.whoNameEn() : "Unspecified",
                    total,
                    calculatePercentage(total, parentTotal),
                    first.whoTechnical(),
                    0,
                    Collections.emptyList()
            );
        }
    }

    private List<TreeNodeDTO> buildTreeNodesByCategory(List<TreeReportProjection> projections, BigDecimal parentTotal) {
        return groupAndMap(projections, TreeReportProjection::categoryId,
                (id, group) -> buildTreeNode(null, group, parentTotal, 2),
                Comparator.comparing(TreeNodeDTO::amount).reversed());
    }

    private List<TreeNodeDTO> buildTreeNodesByWho(List<TreeReportProjection> projections, BigDecimal parentTotal) {
        return groupAndMap(projections,
                p -> Optional.ofNullable(p.whoId()).orElse(-1L),
                (id, group) -> buildTreeNode(null, group, parentTotal, 3),
                Comparator.comparing(TreeNodeDTO::amount).reversed());
    }

    // ============================================
    // PIVOT REPORT - Removed, handled by PivotReportBuilder
    // ============================================

    // ============================================
    // UTILITIES
    // ============================================

    private <T> BigDecimal sumTotals(List<T> list, Function<T, BigDecimal> mapper) {
        return list.stream()
                .map(mapper)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculatePercentage(BigDecimal amount, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        BigDecimal raw = amount.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        return raw.setScale(2, RoundingMode.HALF_UP);
    }

    private <T, K, R> List<R> groupAndMap(List<T> list, Function<T, K> classifier,
                                          BiFunction<K, List<T>, R> mapper,
                                          Comparator<R> comparator) {
        return list.stream()
                .collect(Collectors.groupingBy(classifier))
                .entrySet().stream()
                .map(e -> mapper.apply(e.getKey(), e.getValue()))
                .sorted(comparator)
                .toList();
    }
}