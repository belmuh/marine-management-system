package com.marine.management.modules.finance.presentation.dto.controller;

import com.marine.management.modules.finance.domain.enums.PaymentMethod;
import com.marine.management.modules.finance.domain.enums.RecordType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdateEntryRequest(
        @NotNull RecordType entryType,
        @NotNull UUID categoryId,
        @NotNull String amount,
        @NotNull String currency,
        @NotNull LocalDate entryDate,
        @NotNull PaymentMethod paymentMethod,
        String description,

        // Context fields (nullable)
        Long whoId,
        Long mainCategoryId,
        String recipient,
        String country,
        String city,
        String specificLocation,
        String vendor,

        // Other fields (nullable)
        String receiptNumber,
        String frequency,
        String priority,
        List<String> tags
) {
}
