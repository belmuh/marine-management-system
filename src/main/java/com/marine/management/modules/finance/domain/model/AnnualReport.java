package com.marine.management.modules.finance.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Annual financial report with monthly breakdown.
 *
 * <p>Immutable domain model representing a full year's financial data
 * grouped by category with monthly totals.
 *
 * @param year Report year
 * @param currency Currency code (e.g., EUR, USD)
 * @param monthlyTotals Monthly income/expense/cumulative totals (1-12)
 * @param categoryBreakdowns Category-wise yearly summaries
 */
public record AnnualReport(
        int year,
        String currency,
        Map<Integer, MonthlyTotal> monthlyTotals,
        List<CategoryYearSummary> categoryBreakdowns,
        BigDecimal carryOverBalance
) {
    /**
     * Compact constructor ensuring immutability.
     */
    public AnnualReport {
        monthlyTotals = Map.copyOf(monthlyTotals);
        categoryBreakdowns = List.copyOf(categoryBreakdowns);
    }

    /**
     * Calculates total income for the year.
     *
     * @return Sum of all monthly income
     */
    public BigDecimal getTotalIncome() {
        return monthlyTotals.values().stream()
                .map(MonthlyTotal::income)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates total expense for the year.
     *
     * @return Sum of all monthly expenses (grand total)
     */
    public BigDecimal getTotalExpense() {
        return monthlyTotals.values().stream()
                .map(MonthlyTotal::expense)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates net balance for the year.
     *
     * @return Total income minus total expense
     */
    public BigDecimal getNetBalance() {
        return getTotalIncome().subtract(getTotalExpense());
    }

    /**
     * Gets the grand total (total expenses).
     *
     * Alias for getTotalExpense() for backward compatibility.
     *
     * @return Total expenses for the year
     */
    public BigDecimal getGrandTotal() {
        return getTotalExpense();
    }

    /**
     * Gets the remaining money at year end.
     *
     * Returns the cumulative balance from December (month 12).
     *
     * @return Year-end cumulative balance
     */
    public BigDecimal getRemainingMoney() {
        MonthlyTotal december = monthlyTotals.get(12);
        return december != null ? december.cumulative() : BigDecimal.ZERO;
    }
}