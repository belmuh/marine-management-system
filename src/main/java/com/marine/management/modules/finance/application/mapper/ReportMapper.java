package com.marine.management.modules.finance.application.mapper;

import com.marine.management.modules.finance.domain.model.AnnualReport;
import com.marine.management.modules.finance.domain.model.MonthlyBreakdown;
import com.marine.management.modules.finance.domain.model.PeriodReport;
import com.marine.management.modules.finance.presentation.dto.reports.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ReportMapper {

    public AnnualBreakdownDto toAnnualBreakdownDto(AnnualReport report) {
        List<CategoryMonthlyDataDto> categories = report.getCategoryBreakdowns()
                .stream()
                .map(this::toCategoryMonthlyDataDto)
                .toList();

        List<MonthlyTotalDto> monthlyTotals = report.getMonthlyTotals()
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> toMonthlyTotalDtoFromAnnual(entry.getKey(), entry.getValue()))
                .toList();

        return new AnnualBreakdownDto(
                report.getYear(),
                categories,
                monthlyTotals,
                report.getGrandTotal(),
                report.getRemainingMoney()
        );
    }

    public PeriodBreakdownDto toPeriodBreakdownDto(PeriodReport report) {
        List<CategoryMonthlyDataDto> categories = report.getCategoryBreakdowns()
                .stream()
                .map(this::toCategoryMonthlyDataDto)
                .toList();

        List<MonthlyTotalDto> monthlyTotals = report.getMonthlyTotals()
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> toMonthlyTotalDtoFromPeriod(entry.getKey(), entry.getValue()))
                .toList();

        return new PeriodBreakdownDto(
                report.getPeriod().startDate().toString(),
                report.getPeriod().endDate().toString(),
                categories,
                monthlyTotals,
                report.getTotalIncome(),
                report.getTotalExpense(),
                report.getNetBalance()
        );
    }

    private CategoryMonthlyDataDto toCategoryMonthlyDataDto(MonthlyBreakdown breakdown) {
        Map<String, java.math.BigDecimal> monthlyAmounts = breakdown.getMonthlyAmounts()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()),
                        Map.Entry::getValue
                ));

        return new CategoryMonthlyDataDto(
                breakdown.getCategoryName(),
                monthlyAmounts,
                breakdown.getTotal()
        );
    }

    private MonthlyTotalDto toMonthlyTotalDtoFromAnnual(
            int month,
            AnnualReport.MonthlyTotal total
    ) {
        return new MonthlyTotalDto(
                month,
                total.income(),
                total.expense(),
                total.cumulative()
        );
    }

    private MonthlyTotalDto toMonthlyTotalDtoFromPeriod(
            int month,
            PeriodReport.MonthlyTotal total
    ) {
        return new MonthlyTotalDto(
                month,
                total.income(),
                total.expense(),
                total.cumulative()
        );
    }
}