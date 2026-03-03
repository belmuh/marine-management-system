package com.marine.management.modules.finance.presentation.dto;

import com.marine.management.modules.finance.domain.entities.Payment;
import com.marine.management.modules.finance.domain.enums.PaymentMethod;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for payment history
 */
public record PaymentResponseDto(
        UUID paymentId,
        UUID entryId,
        MoneyDto amount,
        LocalDate paymentDate,
        String paymentReference,
        PaymentMethod paymentMethod,
        String notes,
        UUID recordedById,
        String recordedByName,
        LocalDateTime recordedAt
) {

    public static PaymentResponseDto from(Payment payment) {
        return new PaymentResponseDto(
                payment.getPaymentId(),
                payment.getEntry().getEntryId(),
                MoneyDto.from(payment.getAmount()),
                payment.getPaymentDate(),
                payment.getPaymentReference(),
                payment.getPaymentMethod(),
                payment.getNotes(),
                payment.getRecordedBy().getUserId(),
                payment.getRecordedBy().getFullName(),
                payment.getRecordedAt()
        );
    }
}