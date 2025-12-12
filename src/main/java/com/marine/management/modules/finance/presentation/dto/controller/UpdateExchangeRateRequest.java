package com.marine.management.modules.finance.presentation.dto.controller;

import java.time.LocalDate;

public record UpdateExchangeRateRequest(
        java.math.BigDecimal rate,
        LocalDate rateDate
) {
}
