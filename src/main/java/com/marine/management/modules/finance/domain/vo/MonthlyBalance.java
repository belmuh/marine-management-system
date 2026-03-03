package com.marine.management.modules.finance.domain.vo;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Value Object representing financial balance for a single month.
 *
 * Immutable and contains all monthly financial data.
 */
public class MonthlyBalance {

    private final YearMonth month;
    private final Money income;
    private final Money expense;

    private MonthlyBalance(YearMonth month, Money income, Money expense) {
        this.month = Objects.requireNonNull(month, "Month cannot be null");
        this.income = Objects.requireNonNull(income, "Income cannot be null");
        this.expense = Objects.requireNonNull(expense, "Expense cannot be null");
    }

    /**
     * Factory method to create a MonthlyBalance.
     */
    public static MonthlyBalance of(YearMonth month, Money income, Money expense) {
        return new MonthlyBalance(month, income, expense);
    }

    /**
     * Factory method to create a zero-balance month.
     */
    public static MonthlyBalance zero(YearMonth month, String currency) {
        return new MonthlyBalance(
                month,
                Money.zero(currency),
                Money.zero(currency)
        );
    }

    /**
     * Calculates net balance for this month.
     *
     * @return Net balance (income - expense)
     */
    public Money netBalance() {
        return income.subtract(expense);
    }

    /**
     * Checks if this month has any activity.
     */
    public boolean hasActivity() {
        return !income.isZero() || !expense.isZero();
    }

    /**
     * Checks if this month is in deficit.
     */
    public boolean isDeficit() {
        return expense.isGreaterThan(income);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MonthlyBalance that)) return false;
        return month.equals(that.month) &&
                income.equals(that.income) &&
                expense.equals(that.expense);
    }

    @Override
    public int hashCode() {
        return Objects.hash(month, income, expense);
    }

    @Override
    public String toString() {
        return String.format("MonthlyBalance[%s: income=%s, expense=%s, net=%s]",
                month, income, expense, netBalance());
    }
}