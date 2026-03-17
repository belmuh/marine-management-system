package com.marine.management.modules.finance.domain.model;

import com.marine.management.modules.finance.domain.vo.Period;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Period financial report with monthly breakdown.
 *
 * <p>Immutable domain model representing financial data for a custom date range
 * grouped by category with monthly totals.
 *
 * @param period Time period covered by this report
 * @param categoryBreakdowns Category-wise monthly summaries
 * @param monthlyTotals Monthly income/expense/cumulative totals
 * @param currency Currency code (e.g., EUR, USD)
 */
public record PeriodReport(
        Period period,
        List<MonthlyBreakdown> categoryBreakdowns,
        Map<Integer, MonthlyTotal> monthlyTotals,
        String currency,
        BigDecimal carryOverBalance
) {
    /**
     * Compact constructor ensuring immutability.
     */
    public PeriodReport {
        categoryBreakdowns = List.copyOf(categoryBreakdowns);
        monthlyTotals = Map.copyOf(monthlyTotals);
    }

    /**
     * Calculates total income for the period.
     *
     * @return Sum of all monthly income
     */
    public BigDecimal getTotalIncome() {
        return monthlyTotals.values().stream()
                .map(MonthlyTotal::income)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates total expense for the period.
     *
     * @return Sum of all monthly expenses
     */
    public BigDecimal getTotalExpense() {
        return monthlyTotals.values().stream()
                .map(MonthlyTotal::expense)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates net balance for the period (income - expense, without carry-over).
     *
     * @return Total income minus total expense
     */
    public BigDecimal getNetBalance() {
        return getTotalIncome().subtract(getTotalExpense());
    }

    /**
     * Gets the remaining money at period end.
     *
     * Returns the cumulative balance from the last month of the period.
     * Includes carry-over from prior periods.
     *
     * @return Period-end cumulative balance
     */
    public BigDecimal getRemainingMoney() {
        int lastMonth = period.endDate().getMonthValue();
        MonthlyTotal last = monthlyTotals.get(lastMonth);
        return last != null ? last.cumulative() : BigDecimal.ZERO;
    }
}