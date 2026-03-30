package com.marine.management.modules.organization.application.commands;

import com.marine.management.modules.organization.domain.YachtType;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Command to register a new yacht organization with admin user.
 *
 * Sent as a single API call from the frontend wizard (all 5 steps combined).
 *
 * Creates:
 * - Organization (tenant) with yacht details and financial settings
 * - Admin user (first user, ADMIN role enforced by backend)
 * - Default tenant reference data (MainCategory, WHO selections)
 */
public record RegisterYachtCommand(

        // Step 1 — Yacht Info
        String yachtName,
        YachtType yachtType,
        Integer yachtLength,
        String flagCountry,
        String homeMarina,

        // Step 2 — Company & Admin
        String companyName,
        String email,
        String password,
        String firstName,
        String lastName,
        String phoneNumber,

        // Step 3 — Financial Settings
        String baseCurrency,
        String timezone,
        Integer financialYearStartMonth,
        BigDecimal approvalLimit,
        Boolean managerApprovalEnabled,

        // Step 4 — Category & WHO Selections (null = enable all)
        Set<Long> selectedMainCategoryIds,
        Set<Long> selectedWhoIds
) {
    /**
     * Compact constructor with validation.
     * Validates required fields and business rules.
     */
    public RegisterYachtCommand {

        // ── Step 1: Yacht validation ──

        if (yachtName == null || yachtName.trim().isEmpty()) {
            throw new IllegalArgumentException("Yacht name is required");
        }
        if (yachtName.length() > 100) {
            throw new IllegalArgumentException("Yacht name cannot exceed 100 characters");
        }

        if (yachtType == null) {
            throw new IllegalArgumentException("Yacht type is required");
        }

        if (yachtLength != null) {
            if (yachtLength < 5 || yachtLength > 200) {
                throw new IllegalArgumentException("Yacht length must be between 5 and 200 meters");
            }
        }

        if (flagCountry == null || flagCountry.length() != 2) {
            throw new IllegalArgumentException("Valid flag country (2-letter ISO code) is required");
        }

        // ── Step 2: Admin user validation ──

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (password.length() > 100) {
            throw new IllegalArgumentException("Password cannot exceed 100 characters");
        }

        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }

        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }

        // ── Step 3: Financial settings validation ──

        if (baseCurrency == null || baseCurrency.length() != 3) {
            throw new IllegalArgumentException("Valid currency (3-letter ISO code) is required");
        }

        if (timezone != null && !timezone.trim().isEmpty()) {
            if (!java.time.ZoneId.getAvailableZoneIds().contains(timezone)) {
                throw new IllegalArgumentException("Invalid timezone: " + timezone);
            }
        }

        if (financialYearStartMonth != null) {
            if (financialYearStartMonth < 1 || financialYearStartMonth > 12) {
                throw new IllegalArgumentException("Financial year start month must be between 1 and 12");
            }
        }

        if (approvalLimit != null && approvalLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Approval limit cannot be negative");
        }

        // Defaults
        if (timezone == null || timezone.trim().isEmpty()) {
            timezone = "Europe/Istanbul";
        }
        if (financialYearStartMonth == null) {
            financialYearStartMonth = 1;
        }
        if (managerApprovalEnabled == null) {
            managerApprovalEnabled = false;
        }
    }
}
