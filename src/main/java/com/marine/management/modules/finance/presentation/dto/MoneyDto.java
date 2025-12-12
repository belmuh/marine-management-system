package com.marine.management.modules.finance.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.marine.management.modules.finance.domain.vo.Money;

import java.math.BigDecimal;
import java.util.Currency;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MoneyDto(
        String amount,
        String currency
) {

    public Money toMoney() {
        if (amount == null || currency == null) {
            throw new IllegalArgumentException("Amount and currency cannot be null");
        }

        try {
            BigDecimal amountValue = new BigDecimal(amount);
            Currency currencyInstance = Currency.getInstance(currency);

            return new Money(amountValue, currencyInstance.getCurrencyCode());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount format: " + amount, e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid currency code: " + currency, e);
        }
    }

    public static MoneyDto from(Money money) {
        if (money == null) {
            return null;
        }

        return new MoneyDto(
                money.getAmount().toPlainString(),
                money.getCurrencyCode()
        );
    }

    public BigDecimal getAmountAsBigDecimal() {
        return new BigDecimal(amount);
    }

    public Currency getCurrencyAsCurrency() {
        return Currency.getInstance(currency);
    }
}