package com.marine.management.modules.finance.presentation.dto.reports;

import java.time.LocalDate;
import java.util.List;

public record TreeReportRequest(
        LocalDate startDate,
        LocalDate endDate,
        String currency,
        Long yachtId,
        List<Long> categoryIds,
        List<String> technicalCategories,
        String searchText
) {
    public TreeReportRequest {
        if (currency == null || currency.isBlank()) {
            currency = "EUR";
        }
    }

    // Basit constructor
    public TreeReportRequest(LocalDate startDate, LocalDate endDate) {
        this(startDate, endDate, "EUR", null, null, null, null);
    }

    // Currency ile
    public TreeReportRequest(LocalDate startDate, LocalDate endDate, String currency) {
        this(startDate, endDate, currency, null, null, null, null);
    }
}