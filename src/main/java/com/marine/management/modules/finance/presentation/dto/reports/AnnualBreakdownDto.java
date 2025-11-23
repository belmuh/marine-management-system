package com.marine.management.modules.finance.presentation.dto;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AnnualBreakdownDto(
        int year,
        List<CategoryMonthlyDataDto> categories,
        List<MonthlyTotalDto> monthlyTotals,
        BigDecimal grandTotal,
        BigDecimal remainingMoney
) {}

// CategoryMonthlyDataDto.java
public record CategoryMonthlyDataDto(
        String categoryName,
        Map<String, BigDecimal> monthlyAmounts,  // "1" -> amount, "2" -> amount
        BigDecimal total
) {}

// MonthlyTotalDto.java
public record MonthlyTotalDto(
        int month,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netBalance
) {}

// PeriodBreakdownDto.java
public record PeriodBreakdownDto(
        String startDate,
        String endDate,
        List<CategoryMonthlyDataDto> categories,
        List<MonthlyTotalDto> monthlyTotals,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netBalance
) {}