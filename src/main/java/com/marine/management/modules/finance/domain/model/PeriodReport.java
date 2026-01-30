package com.marine.management.modules.finance.domain.model;

import com.marine.management.modules.finance.domain.vo.Period;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class PeriodReport {
    private final Period period;
    private final List<MonthlyBreakdown> categoryBreakdowns;  // ✅ MonthlyBreakdown
    private final Map<Integer, MonthlyTotal> monthlyTotals;
    private final String currency;

    public PeriodReport(
            Period period,
            List<MonthlyBreakdown> categoryBreakdowns,
            Map<Integer, MonthlyTotal> monthlyTotals,
            String currency
    ) {
        this.period = period;
        this.categoryBreakdowns = List.copyOf(categoryBreakdowns);
        this.monthlyTotals = Map.copyOf(monthlyTotals);
        this.currency = currency;
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

    // Getters
    public Period getPeriod() { return period; }
    public List<MonthlyBreakdown> getCategoryBreakdowns() { return categoryBreakdowns; }
    public Map<Integer, MonthlyTotal> getMonthlyTotals() { return monthlyTotals; }
    public String getCurrency() { return currency; }
}