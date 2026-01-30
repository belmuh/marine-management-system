package com.marine.management.modules.finance.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Annual financial report with monthly breakdown
 */
public class AnnualReport {
    private final int year;
    private final String currency;
    private final Map<Integer, MonthlyTotal> monthlyTotals;
    private final List<CategoryYearSummary> categoryBreakdowns;
    private final BigDecimal grandTotal;       // ⭐ ekledik
    private final BigDecimal remainingMoney;   // ⭐ ekledik

    public AnnualReport(
            int year,
            String currency,
            Map<Integer, MonthlyTotal> monthlyTotals,
            List<CategoryYearSummary> categoryBreakdowns,
            BigDecimal grandTotal,
            BigDecimal remainingMoney
    ) {
        this.year = year;
        this.currency = currency;
        this.monthlyTotals = Map.copyOf(monthlyTotals);
        this.categoryBreakdowns = List.copyOf(categoryBreakdowns);
        this.grandTotal = grandTotal;
        this.remainingMoney = remainingMoney;
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

    // ⭐ Getterlar
    public int getYear() { return year; }
    public String getCurrency() { return currency; }
    public Map<Integer, MonthlyTotal> getMonthlyTotals() { return monthlyTotals; }
    public List<CategoryYearSummary> getCategoryBreakdowns() { return categoryBreakdowns; }
    public BigDecimal getGrandTotal() { return grandTotal; }
    public BigDecimal getRemainingMoney() { return remainingMoney; }
}
