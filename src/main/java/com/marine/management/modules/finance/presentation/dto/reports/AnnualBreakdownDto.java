package com.marine.management.modules.finance.presentation.dto.reports;

import java.math.BigDecimal;
import java.util.List;

public record AnnualBreakdownDto(
        int year,
        List<CategoryMonthlyDataDto> categories,
        List<MonthlyTotalDto> monthlyTotals,
        BigDecimal grandTotal,
        BigDecimal remainingMoney
) {}