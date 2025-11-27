package com.marine.management.modules.finance.presentation.dto.reports;

import java.math.BigDecimal;

public record MonthlyTotalDto(
        int month,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal cumulative
) {}
