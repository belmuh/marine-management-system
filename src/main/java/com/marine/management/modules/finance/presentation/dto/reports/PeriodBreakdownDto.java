package com.marine.management.modules.finance.presentation.dto.reports;

import java.math.BigDecimal;
import java.util.List;

public record PeriodBreakdownDto(
        String startDate,
        String endDate,
        List<CategoryMonthlyDataDto> categories,
        List<MonthlyTotalDto> monthlyTotals,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal cumulative
) {}