package com.marine.management.modules.finance.domain.valueObjects;

import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.Objects;

@Embeddable
public class Money {

    private BigDecimal amount;
    private String currencyCode;

    protected Money() {};

    private Money(BigDecimal amount, String currencyCode) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency code is required");
        }
        this.amount = amount;
        this.currencyCode = currencyCode.toUpperCase();
    }

    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), currencyCode);
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, currencyCode);
    }

    // BUSINESS METHODS

    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currencyCode);
    }

    public Money subtract(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currencyCode);
    }

    public Money multiply(BigDecimal multiplier){
        return new Money(this.amount.multiply(multiplier), this.currencyCode);
    }

    public Money divide(BigDecimal divisor){
        if(divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw  new IllegalArgumentException("Cannot divide by zero");
        }
        return new Money(this.amount.divide(divisor), this.currencyCode);
     }

    // QUERY METHODS

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

    public boolean isLessThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    public boolean hasSameCurrency(Money other) {
        return this.currencyCode.equals(other.currencyCode);
    }

    // GETTERS

    public BigDecimal amount() {
        return amount;
    }

    public String currencyCode() {
        return currencyCode;
    }

    // VALIDATION

    private void validateSameCurrency(Money other) {
        if (!this.currencyCode.equals(other.currencyCode)) {
            throw new IllegalArgumentException(
                    String.format("Currency mismatch: %s vs %s",
                            this.currencyCode, other.currencyCode)
            );
        }
    }

    // EQUALS/HASHCODE/TOSTRING

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
        return String.format("%s %s", amount, currencyCode);
    }

}
