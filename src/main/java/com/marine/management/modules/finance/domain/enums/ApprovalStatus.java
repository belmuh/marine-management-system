package com.marine.management.modules.finance.domain.enums;

/**
 * Status of an approval decision
 */
public enum ApprovalStatus {
    PENDING,    // Awaiting decision
    APPROVED,   // Fully approved
    PARTIAL,    // Partially approved (reduced amount)
    REJECTED,   // Rejected
    RETURNED
}