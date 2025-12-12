package com.marine.management.modules.files;

import com.marine.management.modules.finance.domain.enums.RecordType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a single row from Excel import file
 */
public record ExcelRow(
        LocalDate date,
        String type,           // İşlem türü (açıklama)
        String category,       // Kategori adı
        RecordType entryType,   // INCOME veya EXPENSE
        BigDecimal amount,     // Tutar (pozitif)
        String currency,       // Para birimi (EUR)
        String description,    // Açıklama
        boolean isIncome       // Gelir mi gider mi (backward compatibility)
) {
    // Convenience constructor that infers isIncome from entryType
    public ExcelRow {
        // Validation
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Category cannot be blank");
        }
        if (entryType == null) {
            throw new IllegalArgumentException("Entry type cannot be null");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency cannot be blank");
        }
    }
}