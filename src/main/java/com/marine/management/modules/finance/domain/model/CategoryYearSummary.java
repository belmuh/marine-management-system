package com.marine.management.modules.finance.domain.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Category-wise yearly financial summary.
 *
 * <p>Represents aggregated financial data for a single category
 * broken down by month with yearly total.
 *
 * @param categoryCode Category identifier code
 * @param categoryName Display name of the category
 * @param monthlyValues Monthly amounts (month number 1-12 -> amount)
 * @param yearTotal Sum of all monthly values
 */
public record CategoryYearSummary(
        String categoryCode,
        String categoryName,
        Map<Integer, BigDecimal> monthlyValues,
        BigDecimal yearTotal
) {}