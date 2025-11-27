package com.marine.management.modules.finance.presentation.dto.reports;

import java.math.BigDecimal;
import java.util.Map;

public record CategoryMonthlyDataDto(
        String categoryName,
        Map<String, BigDecimal> monthlyAmounts,  // "1" -> amount, "2" -> amount
        BigDecimal total
) {}