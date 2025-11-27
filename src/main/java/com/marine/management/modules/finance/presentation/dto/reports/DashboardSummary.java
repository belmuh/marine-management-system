package com.marine.management.modules.finance.presentation.dto.reports;

import java.math.BigDecimal;

public record DashboardSummary(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        long incomeCount,
        long expenseCount
) {
    public boolean isProfit() {
        return balance.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isLoss() {
        return balance.compareTo(BigDecimal.ZERO) < 0;
    }
}
