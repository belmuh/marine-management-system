package com.marine.management.modules.finance.domain.model;

import java.math.BigDecimal;
import java.util.*;

public class PivotTreeReport {
    private final int year;
    private final String currency;
    private final List<String> columns;  // ["2024-01", "2024-02", ..., "TOTAL"]
    private final Map<String, BigDecimal> columnTotals;  // Total per column
    private final List<PivotTreeNode> rows;

    public PivotTreeReport(
            int year,
            String currency,
            List<String> columns,
            Map<String, BigDecimal> columnTotals,
            List<PivotTreeNode> rows
    ) {
        this.year = year;
        this.currency = currency;
        this.columns = columns;
        this.columnTotals = columnTotals;
        this.rows = rows;
    }

    public int getYear() { return year; }
    public String getCurrency() { return currency; }
    public List<String> getColumns() { return columns; }
    public Map<String, BigDecimal> getColumnTotals() { return columnTotals; }
    public List<PivotTreeNode> getRows() { return rows; }
}