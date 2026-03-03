package com.marine.management.modules.finance.presentation.dto;

import com.marine.management.modules.finance.domain.entities.FinancialEntry;
import com.marine.management.modules.finance.domain.enums.EntryStatus;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO optimized for JPQL projection (no entity mapping)
 * UPDATED with approval and payment information
 */
public record EntryResponseDto(
        UUID id,
        String entryNumber,
        EntryStatus status,  // 🆕 Status
        RecordType entryType,
        UUID categoryId,
        String categoryCode,
        String categoryName,

        // Requested amounts
        MoneyDto originalAmount,
        MoneyDto baseAmount,

        // Approved amounts
        MoneyDto approvedBaseAmount,

        // Paid amounts
        MoneyDto paidBaseAmount,

        // Derived amounts (calculated)
        MoneyDto remainingAmount,

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
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean hasAttachments
) {

    // JPQL constructor - flat fields (UPDATED)
    public EntryResponseDto(
            UUID id,
            String entryNumber,
            EntryStatus status,
            RecordType entryType,
            UUID categoryId,
            String categoryCode,
            String categoryName,
            BigDecimal originalAmountValue,
            String originalCurrency,
            BigDecimal baseAmountValue,
            String baseCurrency,
            BigDecimal approvedBaseAmountValue,
            String approvedBaseCurrency,
            BigDecimal paidBaseAmountValue,
            String paidBaseCurrency,
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
            String createdByName,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Boolean hasAttachments
    ) {
        this(
                id, entryNumber, status, entryType, categoryId, categoryCode, categoryName,
                new MoneyDto(originalAmountValue.toPlainString(), originalCurrency),
                new MoneyDto(baseAmountValue.toPlainString(), baseCurrency),
                approvedBaseAmountValue != null ? new MoneyDto(approvedBaseAmountValue.toPlainString(), approvedBaseCurrency) : null,
                paidBaseAmountValue != null ? new MoneyDto(paidBaseAmountValue.toPlainString(), paidBaseCurrency) : null,
                null,  // remainingAmount - calculated in from()
                exchangeRate, exchangeRateDate, receiptNumber, description, entryDate,
                paymentMethod, whoId, mainCategoryId, recipient, country, city,
                specificLocation, vendor, createdById, createdByName, createdAt, updatedAt, hasAttachments
        );
    }

    public static EntryResponseDto from(FinancialEntry entry) {
        return fromWithUser(entry, null);
    }

    // Entity mapping (UPDATED)
    public static EntryResponseDto fromWithUser(FinancialEntry entry, String createdByName) {
        return new EntryResponseDto(
                entry.getEntryId(),
                entry.getEntryNumber().getValue(),
                entry.getStatus(),
                entry.getEntryType(),
                entry.getCategory().getId(),
                entry.getCategory().getCode(),
                entry.getCategory().getName(),
                MoneyDto.from(entry.getOriginalAmount()),
                MoneyDto.from(entry.getBaseAmount()),
                entry.getApprovedBaseAmount() != null ? MoneyDto.from(entry.getApprovedBaseAmount()) : null,
                entry.getPaidBaseAmount() != null ? MoneyDto.from(entry.getPaidBaseAmount()) : null,
                entry.getRemainingAmount() != null ? MoneyDto.from(entry.getRemainingAmount()) : null,
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
                createdByName,
                entry.getCreatedAt(),
                entry.getUpdatedAt(),
                entry.hasAttachments()
        );
    }
}