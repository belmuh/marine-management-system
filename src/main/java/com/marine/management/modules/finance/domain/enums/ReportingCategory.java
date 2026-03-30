package com.marine.management.modules.finance.domain.enums;

/**
 * Reporting category for financial entries.
 *
 * Maps entry statuses to their financial reporting meaning:
 *
 *   ACTUAL        → Approved and/or paid entries. Included in financial totals.
 *   COMMITTED     → Pending approval. Shown separately as forecast/pipeline.
 *   NON_FINANCIAL → Draft or rejected. Never included in any financial calculation.
 */
public enum ReportingCategory {
    ACTUAL,
    COMMITTED,
    NON_FINANCIAL
}
