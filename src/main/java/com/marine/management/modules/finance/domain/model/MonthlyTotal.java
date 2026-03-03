package com.marine.management.modules.finance.domain.model;

import java.math.BigDecimal;

/**
 * Monthly financial totals with cumulative balance.
 *
 * <p>Represents aggregated income and expense for a single month
 * along with running cumulative balance.
 *
 * @param income Total income for the month
 * @param expense Total expense for the month
 * @param cumulative Running total balance (sum of all previous months' net changes)
 */
public record MonthlyTotal(
        BigDecimal income,
        BigDecimal expense,
        BigDecimal cumulative
) {}