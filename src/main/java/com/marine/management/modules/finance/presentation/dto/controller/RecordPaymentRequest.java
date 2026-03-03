package com.marine.management.modules.finance.presentation.dto.controller;

import com.marine.management.modules.finance.domain.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for recording a payment
 * Can be called multiple times for partial payments
 */
public record RecordPaymentRequest(
        @NotNull
        @Positive
        BigDecimal amount,  // In base currency (EUR)

        @NotNull
        LocalDate paymentDate,

        String paymentReference,

        @NotNull
        PaymentMethod paymentMethod,

        String notes
) {
}