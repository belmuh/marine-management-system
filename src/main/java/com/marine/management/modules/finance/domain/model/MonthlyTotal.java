package com.marine.management.modules.finance.domain.model;

import java.math.BigDecimal;

public record MonthlyTotal(
        BigDecimal income,
        BigDecimal expense,
        BigDecimal cumulative
) {}