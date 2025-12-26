package com.marine.management.modules.finance.application.dto;

public record TreeReportRequest(
        String period,        // "2024-01" or "2024"
        String periodType,    // "MONTH" or "YEAR"
        String currency       // "EUR"
) { }

