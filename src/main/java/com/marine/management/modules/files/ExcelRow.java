package com.marine.management.modules.files;


import java.math.BigDecimal;
import java.time.LocalDate;

public record ExcelRow(
        LocalDate date,
        String type,        // "Crew Present", "Medicine", "Food And Beverage" vb.
        String category,    // "Market", "General", "RepairTec.Serv", "Marina", "Salary", "income"
        BigDecimal amount,
        String currency,
        String description,
        boolean isIncome
) {
    public boolean isIncome() {
        return isIncome;
    }

    public boolean isExpense() {
        return !isIncome;
    }
}