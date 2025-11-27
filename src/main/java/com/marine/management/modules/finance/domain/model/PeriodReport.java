package com.marine.management.modules.finance.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class PeriodReport {
    private final Period period;
    private final List<MonthlyBreakdown> categoryBreakdowns;
    private final Map<Integer, MonthlyTotal> monthlyTotals;

    public PeriodReport(
            Period period,
            List<MonthlyBreakdown> categoryBreakdowns,
            Map<Integer, MonthlyTotal> monthlyTotals
    ) {
        this.period = period;
        this.categoryBreakdowns = List.copyOf(categoryBreakdowns);
        this.monthlyTotals = Map.copyOf(monthlyTotals);
    }

    public BigDecimal getTotalIncome() {
        return monthlyTotals.values().stream()
                .map(MonthlyTotal::income)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalExpense() {
        return monthlyTotals.values().stream()
                .map(MonthlyTotal::expense)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getNetBalance() {
        return getTotalIncome().subtract(getTotalExpense());
    }

    public Period getPeriod() {
        return period;
    }

    public List<MonthlyBreakdown> getCategoryBreakdowns() {
        return categoryBreakdowns;
    }

    public Map<Integer, MonthlyTotal> getMonthlyTotals() {
        return monthlyTotals;
    }

    public record MonthlyTotal(
            BigDecimal income,
            BigDecimal expense,
            BigDecimal cumulative
    ) { }
}