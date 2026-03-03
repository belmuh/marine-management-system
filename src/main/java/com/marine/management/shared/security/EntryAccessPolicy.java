package com.marine.management.shared.security;

import com.marine.management.modules.finance.application.ApprovalService;
import com.marine.management.modules.finance.domain.entities.FinancialEntry;
import com.marine.management.modules.finance.domain.enums.EntryStatus;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.users.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Data Access Policy for FinancialEntry.
 *
 * Handles Permission and Ownership checks.
 * Tenant isolation is handled automatically by Hibernate filters.
 *
 * Approval Logic:
 * - PENDING_CAPTAIN: Only users with ENTRY_APPROVE_CAPTAIN can approve
 * - PENDING_MANAGER: Users with ENTRY_APPROVE_MANAGER can approve (Manager OR Captain)
 * - Captain has both permissions, so can approve at any level
 *
 * Security layers:
 * 1. Permission check - does user have required permission?
 * 2. Ownership check - is this user's own entry? (when required)
 * 3. Status check - is the operation allowed for this status?
 */
@Component
public class EntryAccessPolicy {

    // ═══════════════════════════════════════════════════════════════════════════
    // SINGLE ENTITY ACCESS CHECKS
    // ═══════════════════════════════════════════════════════════════════════════
    private static final Logger logger = LoggerFactory.getLogger(ApprovalService.class);
    /**
     * Check if user can READ this entry.
     */
    public void checkReadAccess(FinancialEntry entry, User user) {
        // Income check
        if (entry.getEntryType() == RecordType.INCOME) {
            if (!user.getRoleEnum().hasPermission(Permission.INCOME_VIEW)) {
                throw new AccessDeniedException("No permission to view income entries");
            }
            return;
        }

        // Expense check - VIEW_ALL or VIEW_OWN
        if (user.getRoleEnum().hasPermission(Permission.ENTRY_VIEW_ALL)) {
            return;
        }

        if (user.getRoleEnum().hasPermission(Permission.ENTRY_VIEW_OWN)) {
            checkOwnership(entry, user);
            return;
        }

        throw new AccessDeniedException("No permission to view this entry");
    }

    /**
     * Check if user can EDIT this entry.
     */
    public void checkWriteAccess(FinancialEntry entry, User user) {
        EntryStatus status = entry.getStatus();

        // Final status check
        if (status.isFinal()) {
            throw new AccessDeniedException("Cannot modify entry in final status: " + status);
        }

        // Income check
        if (entry.getEntryType() == RecordType.INCOME) {
            if (!user.getRoleEnum().hasPermission(Permission.INCOME_EDIT)) {
                throw new AccessDeniedException("No permission to edit income entries");
            }
            return;
        }

        // Captain/Admin can edit any non-final expense
        if (user.getRoleEnum().hasPermission(Permission.ENTRY_EDIT_ALL)) {
            return;
        }

        // Crew can only edit own DRAFT entries
        if (user.getRoleEnum().hasPermission(Permission.ENTRY_EDIT_OWN)) {
            checkOwnership(entry, user);
            if (!status.canBeEditedByCrew()) {
                throw new AccessDeniedException("You can only edit your own DRAFT entries");
            }
            return;
        }

        throw new AccessDeniedException("No permission to edit this entry");
    }

    /**
     * Check if user can DELETE this entry.
     */
    public void checkDeleteAccess(FinancialEntry entry, User user) {
        // Status check - only DRAFT can be deleted
        if (!entry.getStatus().canBeDeleted()) {
            throw new AccessDeniedException("Only DRAFT entries can be deleted");
        }

        // Income check
        if (entry.getEntryType() == RecordType.INCOME) {
            if (!user.getRoleEnum().hasPermission(Permission.INCOME_DELETE)) {
                throw new AccessDeniedException("No permission to delete income entries");
            }
            return;
        }

        // Captain/Admin can delete any DRAFT
        if (user.getRoleEnum().hasPermission(Permission.ENTRY_DELETE_ALL)) {
            return;
        }

        // Crew can delete own DRAFT
        if (user.getRoleEnum().hasPermission(Permission.ENTRY_DELETE_OWN)) {
            checkOwnership(entry, user);
            return;
        }

        throw new AccessDeniedException("No permission to delete this entry");
    }

    /**
     * Check if user can SUBMIT this entry for approval.
     */
    public void checkSubmitAccess(FinancialEntry entry, User user) {
        if (entry.getStatus() != EntryStatus.DRAFT) {
            throw new AccessDeniedException("Only DRAFT entries can be submitted");
        }

        if (!user.getRoleEnum().hasPermission(Permission.ENTRY_SUBMIT)) {
            throw new AccessDeniedException("No permission to submit entries");
        }

        // Only owner or admin can submit
        if (!user.getRoleEnum().isAdmin()) {
            checkOwnership(entry, user);
        }
    }

    /**
     * Check if user can APPROVE this entry.
     *
     * Logic:
     * - PENDING_CAPTAIN: Requires ENTRY_APPROVE_CAPTAIN (only Captain)
     * - PENDING_MANAGER: Requires ENTRY_APPROVE_MANAGER (Manager OR Captain)
     */
    public void checkApproveAccess(FinancialEntry entry, User user) {
        EntryStatus status = entry.getStatus();

        if (!status.isPending()) {
            throw new AccessDeniedException("Entry is not pending approval, current status: " + status);
        }

        if (status.isPendingCaptain()) {
            // Only Captain can approve PENDING_CAPTAIN
            if (!user.getRoleEnum().hasPermission(Permission.ENTRY_APPROVE_CAPTAIN)) {
                throw new AccessDeniedException("Only Captain can approve at this level");
            }
        } else if (status.isPendingManager()) {
            // Manager OR Captain can approve PENDING_MANAGER
            if (!user.getRoleEnum().hasPermission(Permission.ENTRY_APPROVE_MANAGER)) {
                throw new AccessDeniedException("No permission to approve manager-level entries");
            }
        }
    }

    /**
     * Check if user can REJECT this entry.
     */
    public void checkRejectAccess(FinancialEntry entry, User user) {
        EntryStatus status = entry.getStatus();

        if (!status.isPending()) {
            throw new AccessDeniedException("Only pending entries can be rejected");
        }

        if (!user.getRoleEnum().hasPermission(Permission.ENTRY_REJECT)) {
            throw new AccessDeniedException("No permission to reject entries");
        }

        // Additional: can only reject at your level or if you're Captain
        if (status.isPendingCaptain()) {
            if (!user.getRoleEnum().hasPermission(Permission.ENTRY_APPROVE_CAPTAIN)) {
                throw new AccessDeniedException("Only Captain can reject at this level");
            }
        }
        // Manager or Captain can reject PENDING_MANAGER (both have ENTRY_REJECT)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LIST QUERY SPECIFICATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get Specification for expense list queries based on user permissions.
     */
    public Specification<FinancialEntry> getExpenseReadSpecification(User user) {
        Role role = user.getRoleEnum();

        // Base: only EXPENSE type
        Specification<FinancialEntry> expenseOnly = (root, query, cb) ->
                cb.equal(root.get("entryType"), RecordType.EXPENSE);

        if (role.hasPermission(Permission.ENTRY_VIEW_ALL)) {
            // Captain/Manager: All expenses
            return expenseOnly;
        }

        if (role.hasPermission(Permission.ENTRY_VIEW_OWN)) {
            // Crew: Own expenses only
            return expenseOnly.and((root, query, cb) ->
                    cb.equal(root.get("createdById"), user.getUserId()));
        }

        // No permissions
        return (root, query, cb) -> cb.disjunction();
    }

    /**
     * Get Specification for income list queries.
     */
    public Specification<FinancialEntry> getIncomeReadSpecification(User user) {
        if (!user.getRoleEnum().hasPermission(Permission.INCOME_VIEW)) {
            return (root, query, cb) -> cb.disjunction();
        }

        return (root, query, cb) -> cb.equal(root.get("entryType"), RecordType.INCOME);
    }

    /**
     * Get Specification for pending approval list based on user role.
     *
     * - CREW: Own DRAFT entries (needs to submit)
     * - MANAGER: PENDING_MANAGER entries (their approval queue)
     * - CAPTAIN: All DRAFT + PENDING_CAPTAIN + PENDING_MANAGER (sees everything)
     */
    public Specification<FinancialEntry> getPendingSpecification(User user) {
        Role role = user.getRoleEnum();

        // Captain: Sees everything pending (all DRAFTs + all pending approvals)
        if (role.hasPermission(Permission.ENTRY_APPROVE_CAPTAIN)) {
            return (root, query, cb) -> root.get("status").in(
                    EntryStatus.DRAFT,            // 🆕 Tüm DRAFT'lar
                    EntryStatus.PENDING_CAPTAIN,
                    EntryStatus.PENDING_MANAGER
            );
        }

        logger.info("Role: {}, hasApproveCapt: {}",
                user.getRoleEnum(),
                user.getRoleEnum().hasPermission(Permission.ENTRY_APPROVE_CAPTAIN));

        // Manager: Only PENDING_MANAGER
        if (role.hasPermission(Permission.ENTRY_APPROVE_MANAGER)) {
            return (root, query, cb) -> cb.equal(root.get("status"), EntryStatus.PENDING_MANAGER);
        }

        // Crew: Own DRAFT entries only (REJECTED removed - it's final)
        if (role.hasPermission(Permission.ENTRY_SUBMIT)) {
            return (root, query, cb) -> cb.and(
                    cb.equal(root.get("createdById"), user.getUserId()),
                    cb.equal(root.get("status"), EntryStatus.DRAFT)  // 🆕 Sadece DRAFT
            );
        }

        // No pending items
        return (root, query, cb) -> cb.disjunction();
    }

    /**
     * Get Specification for captain's pending list (PENDING_CAPTAIN only).
     */
    public Specification<FinancialEntry> getPendingCaptainSpecification() {
        return (root, query, cb) -> cb.equal(root.get("status"), EntryStatus.PENDING_CAPTAIN);
    }

    /**
     * Get Specification for manager's pending list (PENDING_MANAGER only).
     */
    public Specification<FinancialEntry> getPendingManagerSpecification() {
        return (root, query, cb) -> cb.equal(root.get("status"), EntryStatus.PENDING_MANAGER);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CAPABILITY CHECKS (for UI)
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean canViewIncomes(User user) {
        return user.getRoleEnum().hasPermission(Permission.INCOME_VIEW);
    }

    public boolean canCreateIncomes(User user) {
        return user.getRoleEnum().hasPermission(Permission.INCOME_CREATE);
    }

    public boolean canViewReports(User user) {
        return user.getRoleEnum().hasPermission(Permission.REPORT_VIEW);
    }

    public boolean canExportReports(User user) {
        return user.getRoleEnum().hasPermission(Permission.REPORT_EXPORT);
    }

    public boolean canApproveCaptainLevel(User user) {
        return user.getRoleEnum().hasPermission(Permission.ENTRY_APPROVE_CAPTAIN);
    }

    public boolean canApproveManagerLevel(User user) {
        return user.getRoleEnum().hasPermission(Permission.ENTRY_APPROVE_MANAGER);
    }

    public boolean canApproveAnyLevel(User user) {
        return canApproveCaptainLevel(user);  // Captain can approve any
    }

    public boolean canManagePayments(User user) {
        return user.getRoleEnum().hasPermission(Permission.PAYMENT_CREATE);
    }

    public boolean canManageUsers(User user) {
        return user.getRoleEnum().hasPermission(Permission.USER_MANAGE);
    }

    public boolean canViewAllEntries(User user) {
        return user.getRoleEnum().hasPermission(Permission.ENTRY_VIEW_ALL);
    }

    public boolean canEditAnyEntry(User user) {
        return user.getRoleEnum().hasPermission(Permission.ENTRY_EDIT_ALL);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private void checkOwnership(FinancialEntry entry, User user) {
        UUID entryCreator = entry.getCreatedById();
        UUID userId = user.getUserId();

        if (entryCreator == null || !entryCreator.equals(userId)) {
            throw new AccessDeniedException("You can only access your own entries");
        }
    }
}