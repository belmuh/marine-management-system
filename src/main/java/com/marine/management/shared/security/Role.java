package com.marine.management.shared.security;

import java.util.*;

/**
 * User roles with hierarchical permission inheritance.
 *
 * Hierarchy:
 *   MANAGER (standalone) → CAPTAIN (extends MANAGER) → SUPER_ADMIN
 *   CREW (standalone, no hierarchy link)
 *
 * Approval Logic:
 * - PENDING_CAPTAIN: Only CAPTAIN can approve
 * - PENDING_MANAGER: MANAGER or CAPTAIN can approve
 * - Captain is "super approver" - can approve at any level
 *
 * Roles explained:
 * - CREW: Basic vessel crew member — creates entries, views/edits own
 * - MANAGER: Approval-only role — sees and approves/rejects PENDING_MANAGER entries
 *            Cannot create entries, cannot access reports/payments/incomes
 * - CAPTAIN: Full tenant access — creates entries, approves at any level, manages everything
 * - ADMIN: Alias for CAPTAIN (backward compatibility)
 * - SUPER_ADMIN: Developer only, cross-tenant access
 */
public enum Role {

    /**
     * CREW - Basic vessel crew member
     * Can create expenses, view/edit own entries, submit for approval
     */
    CREW(null, Set.of(
            Permission.ENTRY_CREATE,
            Permission.ENTRY_VIEW_OWN,
            Permission.ENTRY_EDIT_OWN,
            Permission.ENTRY_DELETE_OWN,
            Permission.ENTRY_SUBMIT,
            Permission.CATEGORY_VIEW
    )),

    /**
     * MANAGER - Approval-only role
     * Can ONLY view and approve/reject entries that reach PENDING_MANAGER status.
     * Full (amount = requested) or partial (amount < requested) approval.
     * Cannot create entries, cannot access reports, payments or incomes.
     */
    MANAGER(null, Set.of(
            Permission.ENTRY_VIEW_ALL,         // Must see entries to approve them
            Permission.ENTRY_APPROVE_MANAGER,  // Approve PENDING_MANAGER entries (full or partial)
            Permission.ENTRY_REJECT            // Reject PENDING_MANAGER entries
    )),

    /**
     * CAPTAIN - Full tenant access (= ADMIN)
     * Can do everything — create entries, approve at any level, manage all resources.
     * Inherits MANAGER's approval permissions and adds full system access.
     */
    CAPTAIN(MANAGER, Set.of(
            // Entry lifecycle (was in CREW — Captain must be able to create/submit)
            Permission.ENTRY_CREATE,
            Permission.ENTRY_SUBMIT,
            Permission.ENTRY_EDIT_ALL,
            Permission.ENTRY_DELETE_ALL,
            Permission.ENTRY_VIEW_OWN,    // Redundant with VIEW_ALL (via MANAGER) but kept for clarity
            Permission.ENTRY_APPROVE_CAPTAIN,  // Captain-level approval
            // ENTRY_APPROVE_MANAGER + ENTRY_REJECT inherited from MANAGER

            // Income (Captain manages income)
            Permission.INCOME_VIEW,
            Permission.INCOME_CREATE,
            Permission.INCOME_EDIT,
            Permission.INCOME_DELETE,

            // Payments
            Permission.PAYMENT_VIEW,
            Permission.PAYMENT_CREATE,
            Permission.PAYMENT_EDIT,
            Permission.PAYMENT_DELETE,

            // Reports
            Permission.REPORT_VIEW,
            Permission.REPORT_EXPORT,

            // Users & Categories & Tenant
            Permission.USER_VIEW,
            Permission.USER_MANAGE,
            Permission.CATEGORY_VIEW,
            Permission.CATEGORY_MANAGE,
            Permission.TENANT_MANAGE
    )),

    /**
     * ADMIN - Alias for CAPTAIN (backward compatibility)
     */
    ADMIN(CAPTAIN, Set.of()),

    /**
     * SUPER_ADMIN - Developer only
     * Full system access across all tenants
     */
    SUPER_ADMIN(CAPTAIN, Set.of(
            Permission.SYSTEM_CONFIG,
            Permission.TENANT_CREATE,
            Permission.TENANT_DELETE,
            Permission.CROSS_TENANT_ACCESS
    ));

    private final Role parent;
    private final Set<Permission> ownPermissions;
    private volatile Set<Permission> allPermissions;  // Lazy initialized, cached

    Role(Role parent, Set<Permission> ownPermissions) {
        this.parent = parent;
        this.ownPermissions = Collections.unmodifiableSet(new HashSet<>(ownPermissions));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PERMISSION METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get all permissions including inherited from parent roles.
     * Result is cached for performance.
     */
    public Set<Permission> getAllPermissions() {
        if (allPermissions == null) {
            synchronized (this) {
                if (allPermissions == null) {
                    Set<Permission> computed = new HashSet<>(ownPermissions);
                    if (parent != null) {
                        computed.addAll(parent.getAllPermissions());
                    }
                    allPermissions = Collections.unmodifiableSet(computed);
                }
            }
        }
        return allPermissions;
    }

    /**
     * Returns all permission names (String) for this role including inherited.
     * Use in auth responses instead of repeating the stream/map/collect chain.
     */
    public Set<String> getPermissionNames() {
        return getAllPermissions().stream()
                .map(Permission::name)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /**
     * Get only this role's own permissions (not inherited).
     */
    public Set<Permission> getOwnPermissions() {
        return ownPermissions;
    }

    /**
     * Check if this role has a specific permission.
     */
    public boolean hasPermission(Permission permission) {
        return getAllPermissions().contains(permission);
    }

    /**
     * Check if this role has any of the given permissions.
     */
    public boolean hasAnyPermission(Permission... permissions) {
        Set<Permission> all = getAllPermissions();
        for (Permission p : permissions) {
            if (all.contains(p)) return true;
        }
        return false;
    }

    /**
     * Check if this role has all of the given permissions.
     */
    public boolean hasAllPermissions(Permission... permissions) {
        Set<Permission> all = getAllPermissions();
        for (Permission p : permissions) {
            if (!all.contains(p)) return false;
        }
        return true;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HIERARCHY METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get the parent role (for inheritance).
     */
    public Role getParent() {
        return parent;
    }

    /**
     * Check if this role is at least the given minimum role in hierarchy.
     * Example: CAPTAIN.isAtLeast(CREW) → true
     */
    public boolean isAtLeast(Role minimumRole) {
        if (this == minimumRole) return true;

        Role current = this.parent;
        while (current != null) {
            if (current == minimumRole) return true;
            current = current.parent;
        }
        return false;
    }

    /**
     * Check if this role is higher than the given role in hierarchy.
     */
    public boolean isHigherThan(Role other) {
        return this != other && this.isAtLeast(other);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ROLE CHECK METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean isCrew() {
        return this == CREW;
    }

    public boolean isManager() {
        return this == MANAGER;
    }

    public boolean isCaptain() {
        return this == CAPTAIN || this == ADMIN;
    }

    public boolean isAdmin() {
        return this == CAPTAIN || this == ADMIN || this == SUPER_ADMIN;
    }

    public boolean isSuperAdmin() {
        return this == SUPER_ADMIN;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CAPABILITY METHODS (permission-based convenience methods)
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean canSubmitEntries() {
        return hasPermission(Permission.ENTRY_SUBMIT);
    }

    public boolean canViewAllEntries() {
        return hasPermission(Permission.ENTRY_VIEW_ALL);
    }

    public boolean canEditAnyEntry() {
        return hasPermission(Permission.ENTRY_EDIT_ALL);
    }

    public boolean canApproveCaptainLevel() {
        return hasPermission(Permission.ENTRY_APPROVE_CAPTAIN);
    }

    public boolean canApproveManagerLevel() {
        return hasPermission(Permission.ENTRY_APPROVE_MANAGER);
    }

    public boolean canApproveAnyLevel() {
        return canApproveCaptainLevel();  // Captain can approve any level
    }

    public boolean canRejectEntries() {
        return hasPermission(Permission.ENTRY_REJECT);
    }

    public boolean canViewIncomes() {
        return hasPermission(Permission.INCOME_VIEW);
    }

    public boolean canManageIncomes() {
        return hasPermission(Permission.INCOME_CREATE);
    }

    public boolean canViewReports() {
        return hasPermission(Permission.REPORT_VIEW);
    }

    public boolean canExportReports() {
        return hasPermission(Permission.REPORT_EXPORT);
    }

    public boolean canManageUsers() {
        return hasPermission(Permission.USER_MANAGE);
    }

    public boolean canManagePayments() {
        return hasPermission(Permission.PAYMENT_CREATE);
    }

    public boolean canManageTenant() {
        return hasPermission(Permission.TENANT_MANAGE);
    }
}