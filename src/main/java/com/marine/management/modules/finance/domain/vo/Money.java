package com.marine.management.modules.finance.domain.vo;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

@Embeddable
public class Money {

    private static final int DEFAULT_SCALE = 2;
    private static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_EVEN;

    private BigDecimal amount;
    private String currencyCode;

    protected Money() {
        // JPA için
    }

    public Money(BigDecimal amount, String currencyCode) {
        validateAmount(amount);
        validateCurrencyCode(currencyCode);

        this.amount = amount.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
        this.currencyCode = currencyCode.toUpperCase();
    }

    // === FACTORY METHODS ===

    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), currencyCode);
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, currencyCode);
    }

    public static Money ofMajor(long amountMajor, String currencyCode) {
        return new Money(BigDecimal.valueOf(amountMajor), currencyCode);
    }

    public static Money ofMinor(long amountMinor, String currencyCode) {
        BigDecimal amount = BigDecimal.valueOf(amountMinor)
                .divide(BigDecimal.valueOf(100), DEFAULT_SCALE, DEFAULT_ROUNDING);
        return new Money(amount, currencyCode);
    }

    public static Money zero(String currencyCode) {
        return new Money(BigDecimal.ZERO, currencyCode);
    }

    // === BUSINESS METHODS ===

    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currencyCode);
    }

    public Money subtract(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currencyCode);
    }

    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier), this.currencyCode);
    }

    public Money multiply(long multiplier) {
        return multiply(BigDecimal.valueOf(multiplier));
    }

    public Money divide(BigDecimal divisor) {
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Cannot divide by zero");
        }
        return new Money(this.amount.divide(divisor, DEFAULT_SCALE, DEFAULT_ROUNDING), this.currencyCode);
    }

    public Money divide(long divisor) {
        return divide(BigDecimal.valueOf(divisor));
    }

    public Money negate() {
        return new Money(this.amount.negate(), this.currencyCode);
    }

    public Money abs() {
        return new Money(this.amount.abs(), this.currencyCode);
    }

    // === CURRENCY CONVERSION ===

    /**
     * Convert this money to another currency using the given exchange rate
     * @param rate Exchange rate (1 unit of this currency = rate units of target currency)
     * @param targetCurrency Target currency code
     * @return New Money in target currency
     */
    public Money convertUsing(BigDecimal rate, String targetCurrency) {
        Objects.requireNonNull(rate, "Exchange rate cannot be null");
        Objects.requireNonNull(targetCurrency, "Target currency cannot be null");

        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive");
        }

        BigDecimal convertedAmount = this.amount
                .multiply(rate)
                .setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);

        return new Money(convertedAmount, targetCurrency);
    }

    /**
     * Check if this money is in Euro currency
     */
    public boolean isEuro() {
        return "EUR".equals(this.currencyCode);
    }

    /**
     * Check if this money is in the specified currency
     */
    public boolean isCurrency(String currency) {
        return this.currencyCode.equalsIgnoreCase(currency);
    }

    // === QUERY METHODS ===

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    public boolean isLessThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    public boolean isLessThanOrEqual(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) <= 0;
    }

    public boolean hasSameCurrency(Money other) {
        return this.currencyCode.equalsIgnoreCase(other.currencyCode);
    }

    // === CONVERSION METHODS ===

    public long getAmountMajor() {
        return amount.longValue();
    }

    public long getAmountMinor() {
        return amount.multiply(BigDecimal.valueOf(100)).longValue();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    // === HELPER METHODS ===

    public String format() {
        try {
            Currency currency = Currency.getInstance(currencyCode);
            return String.format("%s %s",
                    currency.getSymbol(),
                    amount.stripTrailingZeros().toPlainString());
        } catch (IllegalArgumentException e) {
            return String.format("%s %s", currencyCode, amount.toPlainString());
        }
    }

    // === VALIDATION METHODS ===

    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }

    private void validateCurrencyCode(String currencyCode) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency code is required");
        }

        // ISO 4217 currency code validation
        if (currencyCode.length() != 3) {
            throw new IllegalArgumentException("Currency code must be 3 characters (ISO 4217)");
        }

        try {
            Currency.getInstance(currencyCode);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid currency code: " + currencyCode);
        }
    }

    private void validateSameCurrency(Money other) {
        if (!hasSameCurrency(other)) {
            throw new IllegalArgumentException(
                    String.format("Currency mismatch: %s vs %s", this.currencyCode, other.currencyCode)
            );
        }
    }

    // === EQUALS/HASHCODE/TOSTRING ===

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money)) return false;
        Money money = (Money) o;
        return Objects.equals(amount, money.amount) &&
                Objects.equals(currencyCode, money.currencyCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currencyCode);
    }

    @Override
    public String toString() {
        return String.format("Money{amount=%s, currency='%s'}",
                amount.toPlainString(), currencyCode);
    }
}