package com.marine.management.modules.finance.presentation.dto.controller;

import com.marine.management.modules.finance.domain.enums.PaymentMethod;
import com.marine.management.modules.finance.domain.enums.RecordType;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateEntryRequest(
        RecordType entryType,
        UUID categoryId,
        String amount,
        String currency,
        LocalDate entryDate,
        PaymentMethod paymentMethod,
        String description
) {
}
