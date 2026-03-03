package com.marine.management.modules.finance.presentation.dto.controller;

import com.marine.management.modules.finance.domain.enums.EntryStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for changing entry status
 * Used for workflow transitions (DRAFT → PENDING_CAPTAIN, etc.)
 */
public record ChangeStatusRequest(
        @NotNull
        EntryStatus newStatus
) {
}