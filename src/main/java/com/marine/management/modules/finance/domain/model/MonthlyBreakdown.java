package com.marine.management.modules.finance.domain.model;

import java.math.BigDecimal;
import java.util.Map;

public record MonthlyBreakdown(
        String categoryName,
        Map<Integer, BigDecimal> monthlyValues,
        BigDecimal yearTotal
) {}