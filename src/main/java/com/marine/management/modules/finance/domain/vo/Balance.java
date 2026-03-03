package com.marine.management.modules.finance.domain.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Value Object representing an account balance.
 *
 * Unlike Money (which represents transactions and must be positive),
 * Balance represents account state and CAN be negative (deficit).
 *
 * IMMUTABLE - All operations return new instances.
 */
public class Balance {

    private static final BigDecimal CRITICAL_THRESHOLD = new BigDecimal("-10000");
    private static final BigDecimal WARNING_THRESHOLD = new BigDecimal("-5000");
    private static final int DEFAULT_SCALE = 2;
    private static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_EVEN;

    private final BigDecimal amount;
    private final String currency;

    private Balance(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Balance amount cannot be null");
        }
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency code is required");
        }

        this.amount = amount.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
        this.currency = currency.toUpperCase();
    }

    // ============================================
    // FACTORY METHODS
    // ============================================

    /**
     * Creates a Balance with the given amount.
     * Amount CAN be negative (deficit state).
     */
    public static Balance of(BigDecimal amount, String currency) {
        return new Balance(amount, currency);
    }

    /**
     * Creates a Balance from a String amount.
     */
    public static Balance of(String amount, String currency) {
        return new Balance(new BigDecimal(amount), currency);
    }

    /**
     * Creates a zero balance.
     */
    public static Balance zero(String currency) {
        return new Balance(BigDecimal.ZERO, currency);
    }

    /**
     * Creates a Balance from Money.
     */
    public static Balance fromMoney(Money money) {
        return new Balance(money.getAmount(), money.getCurrencyCode());
    }

    // ============================================
    // ARITHMETIC OPERATIONS
    // ============================================

    /**
     * Adds a Money transaction to this balance.
     * Used for income transactions.
     */
    public Balance add(Money money) {
        validateSameCurrency(money.getCurrencyCode());
        return new Balance(this.amount.add(money.getAmount()), this.currency);
    }

    /**
     * Subtracts a Money transaction from this balance.
     * Used for expense transactions.
     * Result can be negative (deficit).
     */
    public Balance subtract(Money money) {
        validateSameCurrency(money.getCurrencyCode());
        return new Balance(this.amount.subtract(money.getAmount()), this.currency);
    }

    /**
     * Adds another balance to this balance.
     */
    public Balance add(Balance other) {
        validateSameCurrency(other.currency);
        return new Balance(this.amount.add(other.amount), this.currency);
    }

    /**
     * Subtracts another balance from this balance.
     */
    public Balance subtract(Balance other) {
        validateSameCurrency(other.currency);
        return new Balance(this.amount.subtract(other.amount), this.currency);
    }

    /**
     * Multiplies this balance by a factor.
     */
    public Balance multiply(BigDecimal factor) {
        return new Balance(this.amount.multiply(factor), this.currency);
    }

    /**
     * Divides this balance by a divisor.
     */
    public Balance divide(BigDecimal divisor) {
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Cannot divide by zero");
        }
        return new Balance(
                this.amount.divide(divisor, DEFAULT_SCALE, DEFAULT_ROUNDING),
                this.currency
        );
    }

    /**
     * Returns the absolute value of this balance.
     */
    public Balance abs() {
        return new Balance(this.amount.abs(), this.currency);
    }

    /**
     * Negates this balance.
     */
    public Balance negate() {
        return new Balance(this.amount.negate(), this.currency);
    }

    // ============================================
    // QUERY METHODS
    // ============================================

    /**
     * Checks if this balance is zero.
     */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Checks if this balance is positive (surplus).
     */
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if this balance is negative (deficit).
     */
    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Checks if this balance is in deficit.
     * Alias for isNegative() - more domain-friendly.
     */
    public boolean isDeficit() {
        return isNegative();
    }

    /**
     * Checks if this balance is in surplus.
     * Alias for isPositive() - more domain-friendly.
     */
    public boolean isSurplus() {
        return isPositive();
    }

    /**
     * Checks if balance is greater than another balance.
     */
    public boolean isGreaterThan(Balance other) {
        validateSameCurrency(other.currency);
        return this.amount.compareTo(other.amount) > 0;
    }

    /**
     * Checks if balance is less than another balance.
     */
    public boolean isLessThan(Balance other) {
        validateSameCurrency(other.currency);
        return this.amount.compareTo(other.amount) < 0;
    }

    /**
     * Checks if balance is greater than or equal to another balance.
     */
    public boolean isGreaterThanOrEqual(Balance other) {
        validateSameCurrency(other.currency);
        return this.amount.compareTo(other.amount) >= 0;
    }

    /**
     * Checks if balance is less than or equal to another balance.
     */
    public boolean isLessThanOrEqual(Balance other) {
        validateSameCurrency(other.currency);
        return this.amount.compareTo(other.amount) <= 0;
    }

    /**
     * Checks if balance is at critical level (configurable threshold).
     * Default: less than -10,000
     */
    public boolean isCritical() {
        return amount.compareTo(CRITICAL_THRESHOLD) < 0;
    }

    /**
     * Checks if balance is at warning level (configurable threshold).
     * Default: less than -5,000
     */
    public boolean isWarning() {
        return amount.compareTo(WARNING_THRESHOLD) < 0 && !isCritical();
    }

    /**
     * Checks if balance is at custom threshold.
     */
    public boolean isLessThan(BigDecimal threshold) {
        return amount.compareTo(threshold) < 0;
    }

    // ============================================
    // CONVERSION METHODS
    // ============================================

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrencyCode() {
        return currency;
    }

    /**
     * Converts to Money (only if positive).
     * Throws exception if balance is negative.
     */
    public Money toMoney() {
        if (isNegative()) {
            throw new IllegalStateException(
                    "Cannot convert negative balance to Money: " + this
            );
        }
        return Money.of(amount, currency);
    }

    /**
     * Returns absolute amount as Money.
     * Useful for displaying deficit as positive value.
     */
    public Money toAbsoluteMoney() {
        return Money.of(amount.abs(), currency);
    }

    // ============================================
    // FORMATTING
    // ============================================

    /**
     * Formats balance with currency symbol.
     * Negative values shown with minus sign.
     */
    public String format() {
        try {
            Currency curr = Currency.getInstance(currency);
            return String.format("%s %s",
                    curr.getSymbol(),
                    amount.stripTrailingZeros().toPlainString());
        } catch (IllegalArgumentException e) {
            return String.format("%s %s", currency, amount.toPlainString());
        }
    }

    /**
     * Formats balance with status indicator.
     * Example: "+€1,500.00 (Surplus)" or "-€500.00 (Deficit)"
     */
    public String formatWithStatus() {
        String sign = isPositive() ? "+" : "";
        String status = isPositive() ? "Surplus" : "Deficit";
        return String.format("%s%s (%s)", sign, format(), status);
    }

    // ============================================
    // VALIDATION
    // ============================================

    private void validateSameCurrency(String otherCurrency) {
        if (!this.currency.equalsIgnoreCase(otherCurrency)) {
            throw new IllegalArgumentException(
                    String.format("Currency mismatch: %s vs %s",
                            this.currency, otherCurrency)
            );
        }
    }

    // ============================================
    // COMPARISON
    // ============================================

    /**
     * Compares this balance with another balance.
     * Returns negative if this < other, 0 if equal, positive if this > other.
     */
    public int compareTo(Balance other) {
        validateSameCurrency(other.currency);
        return this.amount.compareTo(other.amount);
    }

    // ============================================
    // EQUALS/HASHCODE/TOSTRING
    // ============================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Balance)) return false;
        Balance balance = (Balance) o;
        return Objects.equals(amount, balance.amount) &&
                Objects.equals(currency, balance.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        String status = isDeficit() ? "DEFICIT" : isSurplus() ? "SURPLUS" : "ZERO";
        return String.format("Balance{amount=%s, currency='%s', status=%s}",
                amount.toPlainString(), currency, status);
    }
}