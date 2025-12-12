package com.marine.management.modules.finance.presentation.dto;

import com.marine.management.modules.finance.domain.entity.FinancialEntry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EntryResponseDto(
        UUID id,
        String entryNumber,
        String entryType,
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
        String paymentMethod,
        Long whoId,
        Long mainCategoryId,
        String recipient,
        String country,
        String city,
        String specificLocation,
        String vendor,
        String createdBy,
        String createdAt,
        String updatedAt,
        boolean hasAttachments
) {

    public static EntryResponseDto from(FinancialEntry entry) {
        return new EntryResponseDto(
                entry.getId(),
                entry.getEntryNumber().getValue(),
                entry.getEntryType().name(),
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
                entry.getPaymentMethod().name(),
                entry.getWhoId(),
                entry.getMainCategoryId(),
                entry.getRecipient(),
                entry.getCountry(),
                entry.getCity(),
                entry.getSpecificLocation(),
                entry.getVendor(),
                entry.getCreatedBy().getUsername(),
                entry.getCreatedAt().toString(),
                entry.getUpdatedAt().toString(),
                entry.hasAttachments()
        );
    }

}