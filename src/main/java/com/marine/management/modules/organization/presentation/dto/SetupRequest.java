package com.marine.management.modules.organization.presentation.dto;

import com.marine.management.modules.organization.domain.YachtType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Setup request DTO - completes onboarding after registration + email verification.
 * Contains yacht details, financial settings, and category selections.
 */
public record SetupRequest(
    // Yacht details
    YachtType yachtType,

    @Min(value = 5, message = "Yacht length must be at least 5 meters")
    @Max(value = 200, message = "Yacht length cannot exceed 200 meters")
    Integer yachtLength,

    @NotBlank(message = "Flag country is required")
    @Size(min = 2, max = 2, message = "Flag country must be 2-character ISO code")
    String flagCountry,

    String homeMarina,

    String companyName,

    // Financial settings
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3-character ISO code")
    String baseCurrency,

    String timezone,

    @Min(1) @Max(12)
    Integer financialYearStartMonth,

    BigDecimal approvalLimit,
    Boolean managerApprovalEnabled,

    // Category & WHO selections (null = enable all)
    Set<Long> selectedMainCategoryIds,
    Set<Long> selectedWhoIds
) {}
