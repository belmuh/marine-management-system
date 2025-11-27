package com.marine.management.modules.finance.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AnnualReport {
    private final int year;
    private final List<MonthlyBreakdown> categoryBreakdowns;
    private final Map<Integer, MonthlyTotal> monthlyTotals;

    public AnnualReport(
            int year,
            List<MonthlyBreakdown> categoryBreakdowns,
            Map<Integer, MonthlyTotal> monthlyTotals
    ) {
        this.year = year;
        this.categoryBreakdowns = List.copyOf(categoryBreakdowns);
        this.monthlyTotals = Map.copyOf(monthlyTotals);
    }

    public BigDecimal getGrandTotal() {
        return categoryBreakdowns.stream()
                .map(MonthlyBreakdown::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalIncome() {
        return monthlyTotals.values().stream()
                .map(MonthlyTotal::income)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getRemainingMoney() {
        return getTotalIncome().subtract(getGrandTotal());
    }

    public int getYear() {
        return year;
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
    ) {
    }
}