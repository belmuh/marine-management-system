package com.marine.management.modules.finance.application.mapper;

import com.marine.management.modules.finance.domain.model.MonthlyBreakdown;
import com.marine.management.modules.finance.domain.model.MonthlyTotal;
import com.marine.management.modules.finance.domain.model.PeriodReport;
import com.marine.management.modules.finance.presentation.dto.reports.CategoryMonthlyDataDto;
import com.marine.management.modules.finance.presentation.dto.reports.MonthlyTotalDto;
import com.marine.management.modules.finance.presentation.dto.reports.PeriodBreakdownDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps PeriodReport domain model to DTOs.
 */
@Component
public class PeriodReportMapper {

    public PeriodBreakdownDto toDto(PeriodReport report) {
        return new PeriodBreakdownDto(
                report.period().startDate().toString(),
                report.period().endDate().toString(),
                mapCategoryBreakdowns(report.categoryBreakdowns()),
                mapMonthlyTotals(report.monthlyTotals()),
                report.getTotalExpense(),
                report.getRemainingMoney(),
                report.carryOverBalance()
        );
    }

    private List<CategoryMonthlyDataDto> mapCategoryBreakdowns(
            List<MonthlyBreakdown> breakdowns) {
        return breakdowns.stream()
                .map(breakdown -> {
                    Map<String, BigDecimal> monthlyAmounts =
                            convertMonthlyValuesToStringKeys(breakdown.monthlyValues());

                    return new CategoryMonthlyDataDto(
                            breakdown.categoryName(),
                            monthlyAmounts,
                            breakdown.yearTotal()
                    );
                })
                .sorted(Comparator.comparing(CategoryMonthlyDataDto::categoryName))
                .toList();
    }

    private List<MonthlyTotalDto> mapMonthlyTotals(Map<Integer, MonthlyTotal> totals) {
        return totals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new MonthlyTotalDto(
                        e.getKey(),
                        e.getValue().income(),
                        e.getValue().expense(),
                        e.getValue().cumulative()
                ))
                .toList();
    }

    private Map<String, BigDecimal> convertMonthlyValuesToStringKeys(
            Map<Integer, BigDecimal> monthlyValues) {
        return monthlyValues.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> String.valueOf(e.getKey()),
                        Map.Entry::getValue
                ));
    }
}