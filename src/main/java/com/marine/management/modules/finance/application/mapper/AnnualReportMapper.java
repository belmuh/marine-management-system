package com.marine.management.modules.finance.application.mapper;

import com.marine.management.modules.finance.domain.model.AnnualReport;
import com.marine.management.modules.finance.domain.model.CategoryYearSummary;
import com.marine.management.modules.finance.domain.model.MonthlyTotal;
import com.marine.management.modules.finance.presentation.dto.reports.AnnualBreakdownDto;
import com.marine.management.modules.finance.presentation.dto.reports.CategoryMonthlyDataDto;
import com.marine.management.modules.finance.presentation.dto.reports.MonthlyTotalDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps AnnualReport domain model to DTOs.
 */
@Component
public class AnnualReportMapper {

    public AnnualBreakdownDto toDto(AnnualReport report) {
        return new AnnualBreakdownDto(
                report.year(),
                mapCategoryBreakdowns(report.categoryBreakdowns()),
                mapMonthlyTotals(report.monthlyTotals()),
                report.getGrandTotal(),
                report.getRemainingMoney(),
                report.carryOverBalance()
        );
    }

    private List<CategoryMonthlyDataDto> mapCategoryBreakdowns(
            List<CategoryYearSummary> breakdowns) {
        return breakdowns.stream()
                .map(b -> new CategoryMonthlyDataDto(
                        b.categoryName(),
                        convertMonthlyValuesToStringKeys(b.monthlyValues()),
                        b.yearTotal()
                ))
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