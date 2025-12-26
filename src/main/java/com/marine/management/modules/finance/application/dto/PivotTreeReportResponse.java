package com.marine.management.modules.finance.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PivotTreeReportResponse(
        int year,
        String currency,
        List<String> columns,
        Map<String, BigDecimal> columnTotals,
        List<PivotTreeNodeDTO> rows
) { }