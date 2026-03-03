package com.marine.management.modules.finance.domain.vo;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Value Object representing cumulative balance at a specific point in time.
 *
 * Contains monthly data plus running cumulative total.
 * Immutable.
 */
public class CumulativeBalance {

    private final YearMonth month;
    private final Money income;
    private final Money expense;
    private final Balance cumulativeBalance; // ✅ Changed from Money to Balance

    private CumulativeBalance(
            YearMonth month,
            Money income,
            Money expense,
            Balance cumulativeBalance // ✅ Balance instead of Money
    ) {
        this.month = Objects.requireNonNull(month, "Month cannot be null");
        this.income = Objects.requireNonNull(income, "Income cannot be null");
        this.expense = Objects.requireNonNull(expense, "Expense cannot be null");
        this.cumulativeBalance = Objects.requireNonNull(cumulativeBalance, "Cumulative balance cannot be null");
    }

    /**
     * Factory method to create from MonthlyBalance + cumulative balance.
     */
    public static CumulativeBalance of(MonthlyBalance monthly, Balance cumulativeBalance) {
        return new CumulativeBalance(
                monthly.getMonth(),
                monthly.getIncome(),
                monthly.getExpense(),
                cumulativeBalance
        );
    }

    /**
     * Factory method for initial state (zero cumulative).
     */
    public static CumulativeBalance initial(YearMonth month, String currency) {
        return new CumulativeBalance(
                month,
                Money.zero(currency),
                Money.zero(currency),
                Balance.zero(currency) // ✅ Balance.zero
        );
    }

    /**
     * Checks if cumulative balance is in deficit.
     */
    public boolean isDeficit() {
        return cumulativeBalance.isDeficit(); // ✅ Balance method
    }

    /**
     * Checks if cumulative balance is at critical level.
     *
     * Critical level is defined as less than -10,000 EUR.
     */
    public boolean isCritical() {
        return cumulativeBalance.isCritical(); // ✅ Balance method
    }

    /**
     * Checks if cumulative balance is at warning level.
     *
     * Warning level is defined as less than -5,000 EUR.
     */
    public boolean isWarning() {
        return cumulativeBalance.isWarning(); // ✅ Balance method
    }

    /**
     * Gets the monthly net change for this period.
     */
    public Money monthlyNetChange() {
        return income.subtract(expense);
    }

    /**
     * Calculates percentage change from previous cumulative balance.
     */
    public BigDecimal percentageChangeTo(Balance previousCumulative) { // ✅ Balance parameter
        if (previousCumulative.isZero()) {
            return BigDecimal.ZERO;
        }

        Balance change = cumulativeBalance.subtract(previousCumulative);
        return change.getAmount()
                .divide(previousCumulative.abs().getAmount(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    // Getters
    public YearMonth getMonth() {
        return month;
    }

    public Money getIncome() {
        return income;
    }

    public Money getExpense() {
        return expense;
    }

    public Balance getCumulativeBalance() { // ✅ Returns Balance
        return cumulativeBalance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CumulativeBalance that)) return false;
        return month.equals(that.month) &&
                cumulativeBalance.equals(that.cumulativeBalance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(month, cumulativeBalance);
    }

    @Override
    public String toString() {
        return String.format(
                "CumulativeBalance[%s: income=%s, expense=%s, cumulative=%s, status=%s]",
                month,
                income,
                expense,
                cumulativeBalance,
                isDeficit() ? (isCritical() ? "CRITICAL" : "DEFICIT") : "SURPLUS"
        );
    }
}