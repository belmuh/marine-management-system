package com.marine.management.modules.finance.application.dto;

import java.math.BigDecimal;

/**
 * DTO for cumulative balance data.
 *
 * Contains monthly financial data plus cumulative total and status flags.
 * Used for API responses and frontend consumption.
 *
 * @param month Month in YYYY-MM format (e.g., "2025-08")
 * @param income Monthly income amount
 * @param expense Monthly expense amount
 * @param cumulativeBalance Running total balance
 * @param isDeficit True if cumulative balance is negative
 * @param isCritical True if cumulative balance is below critical threshold (-10K)
 * @param isWarning True if cumulative balance is below warning threshold (-5K)
 */
public record CumulativeBalanceDTO(
        String month,
        BigDecimal income,
        BigDecimal expense,
        BigDecimal cumulativeBalance,
        boolean isDeficit,
        boolean isCritical,
        boolean isWarning
) {
    /**
     * Validation constructor ensuring non-null required fields.
     *
     * @throws IllegalArgumentException if any required field is null or invalid
     */
    public CumulativeBalanceDTO {
        if (month == null || month.isBlank()) {
            throw new IllegalArgumentException("Month cannot be null or blank");
        }
        if (income == null) {
            throw new IllegalArgumentException("Income cannot be null");
        }
        if (expense == null) {
            throw new IllegalArgumentException("Expense cannot be null");
        }
        if (cumulativeBalance == null) {
            throw new IllegalArgumentException("Cumulative balance cannot be null");
        }
    }

    /**
     * Factory method for zero balance month.
     *
     * @param month Month in YYYY-MM format
     * @return CumulativeBalanceDTO with zero values
     */
    public static CumulativeBalanceDTO zero(String month) {
        return new CumulativeBalanceDTO(
                month,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false,
                false,
                false
        );
    }

    /**
     * Calculates monthly net change (income - expense).
     *
     * @return Net change for the month
     */
    public BigDecimal monthlyNetChange() {
        return income.subtract(expense);
    }
}