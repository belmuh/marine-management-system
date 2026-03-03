package com.marine.management.modules.finance.presentation.dto.controller;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for rejecting an entry
 */
public record RejectEntryRequest(
        @NotBlank
        String rejectionReason
) {
}