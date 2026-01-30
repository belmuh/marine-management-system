package com.marine.management.modules.finance.presentation.dto;

import com.marine.management.modules.finance.domain.entity.FinancialEntry;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO optimized for JPQL projection (no entity mapping)
 */
public record EntryResponseDto(
        UUID id,
        String entryNumber,
        RecordType entryType,
        UUID categoryId,
        String categoryCode,
        String categoryName,
        MoneyDto originalAmount,
        MoneyDto baseAmount,
        BigDecimal exchangeRate,
        LocalDate exchangeRateDate,
        String receiptNumber,
        String description,
        LocalDate entryDate,
        PaymentMethod paymentMethod,
        Long whoId,
        Long mainCategoryId,
        String recipient,
        String country,
        String city,
        String specificLocation,
        String vendor,
        UUID createdById,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean hasAttachments
) {

    // JPQL constructor - flat fields
    public EntryResponseDto(
            UUID id,
            String entryNumber,
            RecordType entryType,
            UUID categoryId,
            String categoryCode,
            String categoryName,
            BigDecimal originalAmountValue,
            String originalCurrency,
            BigDecimal baseAmountValue,
            String baseCurrency,
            BigDecimal exchangeRate,
            LocalDate exchangeRateDate,
            String receiptNumber,
            String description,
            LocalDate entryDate,
            PaymentMethod paymentMethod,
            Long whoId,
            Long mainCategoryId,
            String recipient,
            String country,
            String city,
            String specificLocation,
            String vendor,
            UUID createdById,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Boolean hasAttachments
    ) {
        this(
                id, entryNumber, entryType, categoryId, categoryCode, categoryName,
                new MoneyDto(originalAmountValue.toPlainString(), originalCurrency),
                new MoneyDto(baseAmountValue.toPlainString(), baseCurrency),
                exchangeRate, exchangeRateDate, receiptNumber, description, entryDate,
                paymentMethod, whoId, mainCategoryId, recipient, country, city,
                specificLocation, vendor, createdById, createdAt, updatedAt, hasAttachments
        );
    }

    // Entity mapping
    public static EntryResponseDto from(FinancialEntry entry) {
        return new EntryResponseDto(
                entry.getEntryId(),
                entry.getEntryNumber().getValue(),
                entry.getEntryType(),
                entry.getCategory().getId(),
                entry.getCategory().getCode(),
                entry.getCategory().getName(),
                MoneyDto.from(entry.getOriginalAmount()),
                MoneyDto.from(entry.getBaseAmount()),
                entry.getExchangeRate(),
                entry.getExchangeRateDate(),
                entry.getReceiptNumber(),
                entry.getDescription(),
                entry.getEntryDate(),
                entry.getPaymentMethod(),
                entry.getWhoId(),
                entry.getMainCategoryId(),
                entry.getRecipient(),
                entry.getCountry(),
                entry.getCity(),
                entry.getSpecificLocation(),
                entry.getVendor(),
                entry.getCreatedById(),
                entry.getCreatedAt(),
                entry.getUpdatedAt(),
                entry.hasAttachments()
        );
    }
}