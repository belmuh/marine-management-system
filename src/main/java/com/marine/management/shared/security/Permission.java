package com.marine.management.shared.security;

/**
 * Application permissions for fine-grained access control.
 *
 * Naming convention: {RESOURCE}_{ACTION}_{SCOPE}
 * - RESOURCE: ENTRY, INCOME, REPORT, USER, etc.
 * - ACTION: VIEW, CREATE, EDIT, DELETE, APPROVE, etc.
 * - SCOPE: OWN, ALL, CAPTAIN, MANAGER (optional)
 *
 * Usage:
 * - Role enum defines which permissions each role has
 * - EntryAccessPolicy uses permissions for data access decisions
 * - @PreAuthorize("hasAuthority('ENTRY_APPROVE_CAPTAIN')") in controllers
 */
public enum Permission {

    // ═══════════════════════════════════════════════════════════════════════════
    // ENTRY permissions
    // ═══════════════════════════════════════════════════════════════════════════

    /** Create new financial entries */
    ENTRY_CREATE,

    /** View own entries only */
    ENTRY_VIEW_OWN,

    /** View all entries in tenant */
    ENTRY_VIEW_ALL,

    /** Edit own entries only (DRAFT status) */
    ENTRY_EDIT_OWN,

    /** Edit all entries in tenant (non-final status) */
    ENTRY_EDIT_ALL,

    /** Delete own DRAFT entries */
    ENTRY_DELETE_OWN,

    /** Delete any DRAFT entry */
    ENTRY_DELETE_ALL,

    /** Submit entry for approval (DRAFT → PENDING_CAPTAIN) */
    ENTRY_SUBMIT,

    /** Approve at captain level (PENDING_CAPTAIN → APPROVED/PENDING_MANAGER) */
    ENTRY_APPROVE_CAPTAIN,

    /** Approve at manager level (PENDING_MANAGER → APPROVED) */
    ENTRY_APPROVE_MANAGER,

    /** Reject entries (PENDING_* → REJECTED) */
    ENTRY_REJECT,

    // ═══════════════════════════════════════════════════════════════════════════
    // INCOME permissions
    // ═══════════════════════════════════════════════════════════════════════════

    /** View income entries */
    INCOME_VIEW,

    /** Create income entries */
    INCOME_CREATE,

    /** Edit income entries */
    INCOME_EDIT,

    /** Delete income entries */
    INCOME_DELETE,

    // ═══════════════════════════════════════════════════════════════════════════
    // PAYMENT permissions
    // ═══════════════════════════════════════════════════════════════════════════

    /** View payments */
    PAYMENT_VIEW,

    /** Record payments */
    PAYMENT_CREATE,

    /** Edit payments */
    PAYMENT_EDIT,

    /** Delete payments */
    PAYMENT_DELETE,

    // ═══════════════════════════════════════════════════════════════════════════
    // REPORT permissions
    // ═══════════════════════════════════════════════════════════════════════════

    /** View reports and dashboard */
    REPORT_VIEW,

    /** Export reports (Excel, PDF) */
    REPORT_EXPORT,

    // ═══════════════════════════════════════════════════════════════════════════
    // USER management permissions
    // ═══════════════════════════════════════════════════════════════════════════

    /** View users in tenant */
    USER_VIEW,

    /** Create/edit/deactivate users */
    USER_MANAGE,

    // ═══════════════════════════════════════════════════════════════════════════
    // CATEGORY management permissions
    // ═══════════════════════════════════════════════════════════════════════════

    /** View categories */
    CATEGORY_VIEW,

    /** Manage categories (create/edit/delete) */
    CATEGORY_MANAGE,

    // ═══════════════════════════════════════════════════════════════════════════
    // TENANT permissions
    // ═══════════════════════════════════════════════════════════════════════════

    /** Manage tenant settings */
    TENANT_MANAGE,

    // ═══════════════════════════════════════════════════════════════════════════
    // SUPER ADMIN permissions (Developer only)
    // ═══════════════════════════════════════════════════════════════════════════

    /** System configuration */
    SYSTEM_CONFIG,

    /** Create new tenants */
    TENANT_CREATE,

    /** Delete tenants */
    TENANT_DELETE,

    /** Access data across all tenants */
    CROSS_TENANT_ACCESS;

    /**
     * Get permission category (first part of name)
     */
    public String getCategory() {
        return name().split("_")[0];
    }

    /**
     * Get permission action (second part of name)
     */
    public String getAction() {
        String[] parts = name().split("_");
        return parts.length > 1 ? parts[1] : "";
    }

    /**
     * Get permission scope if exists (third part of name)
     */
    public String getScope() {
        String[] parts = name().split("_");
        return parts.length > 2 ? parts[2] : "";
    }
}