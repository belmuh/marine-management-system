package com.marine.management.modules.finance.domain.model;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class MonthlyBreakdown {
    private final String categoryName;
    private final Map<Integer, BigDecimal> monthlyAmounts;

    public MonthlyBreakdown(String categoryName) {
        this.categoryName = categoryName;
        this.monthlyAmounts = new HashMap<>();
    }

    public void addAmount(int month, BigDecimal amount) {
        validateMonth(month);
        monthlyAmounts.merge(month, amount, BigDecimal::add);
    }

    public BigDecimal getAmountForMonth(int month) {
        return monthlyAmounts.getOrDefault(month, BigDecimal.ZERO);
    }

    public BigDecimal getTotal() {
        return monthlyAmounts.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String getCategoryName() {
        return categoryName;
    }

    public Map<Integer, BigDecimal> getMonthlyAmounts() {
        return new HashMap<>(monthlyAmounts);
    }

    private void validateMonth(int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
    }
}