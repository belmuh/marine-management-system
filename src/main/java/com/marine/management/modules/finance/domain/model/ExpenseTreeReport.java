package com.marine.management.modules.finance.domain.model;

import com.marine.management.modules.finance.domain.vo.Period;

import java.math.BigDecimal;
import java.util.List;

public class ExpenseTreeReport {
    private final Period period;
    private final String currency;
    private final BigDecimal totalAmount;
    private final List<TreeNode> nodes;

    public ExpenseTreeReport(
            Period period,
            String currency,
            BigDecimal totalAmount,
            List<TreeNode> nodes
    ) {
        this.period = period;
        this.currency = currency;
        this.totalAmount = totalAmount;
        this.nodes = nodes;
    }

    // Getters
    public Period getPeriod() { return period; }
    public String getCurrency() { return currency; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public List<TreeNode> getNodes() { return nodes; }
}