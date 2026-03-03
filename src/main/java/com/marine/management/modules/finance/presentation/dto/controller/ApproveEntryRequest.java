package com.marine.management.modules.finance.presentation.dto.controller;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request DTO for approving an entry
 * Used by Captain/Manager to set approved amount
 */
public record ApproveEntryRequest(
        @NotNull
        @Positive
        BigDecimal approvedAmount,  // In base currency (EUR)

        String comments
) {
}