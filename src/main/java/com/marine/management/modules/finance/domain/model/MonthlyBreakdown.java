package com.marine.management.modules.finance.domain.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Monthly breakdown for a single category.
 *
 * <p>Represents financial data for one category broken down by month
 * within a custom time period.
 *
 * @param categoryName Display name of the category
 * @param monthlyValues Monthly amounts (month number 1-12 -> amount)
 * @param yearTotal Sum of all monthly values
 */
public record MonthlyBreakdown(
        String categoryName,
        Map<Integer, BigDecimal> monthlyValues,
        BigDecimal yearTotal
) {}