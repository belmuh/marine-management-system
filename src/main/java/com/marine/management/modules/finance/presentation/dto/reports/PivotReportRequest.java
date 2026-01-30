package com.marine.management.modules.finance.presentation.dto.reports;

import java.util.List;

public record PivotReportRequest(
        int year,
        String currency,
        Long yachtId,
        List<Long> categoryIds
) {
    public PivotReportRequest {
        if (currency == null || currency.isBlank()) {
            currency = "EUR";
        }
        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("Invalid year: " + year);
        }
    }

    public PivotReportRequest(int year) {
        this(year, "EUR", null, null);
    }
}