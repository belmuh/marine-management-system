package com.marine.management.modules.finance.domain.model;

import java.math.BigDecimal;
import java.util.Map;

public record CategoryYearSummary(
        String categoryCode,
        String categoryName,
        Map<Integer, BigDecimal> monthlyValues,
        BigDecimal yearTotal
) {}