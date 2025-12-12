package com.marine.management.modules.finance.presentation.dto.controller;

import com.marine.management.modules.finance.domain.enums.PaymentMethod;
import com.marine.management.modules.finance.domain.enums.RecordType;

import java.time.LocalDate;
import java.util.UUID;

public record CreateEntryRequest(
        RecordType entryType,
        UUID categoryId,
        String amount,
        String currency,
        LocalDate entryDate,
        PaymentMethod paymentMethod,
        String description,
        Long whoId,
        Long mainCategoryId,
        String recipient,
        String country,
        String city,
        String specificLocation,
        String vendor
) {
}
