package com.marine.management.modules.finance.domain.enums;

import java.util.Set;

/**
 * Financial entry status with state machine transitions.
 *
 * Flow:
 *   DRAFT → PENDING_CAPTAIN ──┬──► APPROVED (≤ limit) ──► PAID
 *                             │
 *                             ├──► PENDING_MANAGER (> limit) ──► APPROVED ──► PAID
 *                             │           │
 *                             │           └──► REJECTED
 *                             │
 *                             └──► REJECTED
 *
 * Approval Logic:
 * - All entries go to PENDING_CAPTAIN first
 * - Captain approves: if amount > limit AND managerApprovalEnabled → PENDING_MANAGER
 * - Captain approves: otherwise → APPROVED
 * - Manager can approve PENDING_MANAGER entries
 * - Captain can also approve PENDING_MANAGER entries (super approver)
 *
 * Terminal states: PAID, REJECTED
 */
public enum EntryStatus {

    /**
     * DRAFT - Entry created but not submitted
     * Crew can edit and delete
     */
    DRAFT {
        @Override
        public Set<EntryStatus> allowedTransitions() {
            return Set.of(PENDING_CAPTAIN);
        }

        @Override
        public boolean canBeEditedByCrew() {
            return true;
        }

        @Override
        public boolean canBeDeleted() {
            return true;
        }
    },

    /**
     * PENDING_CAPTAIN - Waiting for Captain approval
     * Captain can approve, reject, or send to manager (if > limit)
     */
    PENDING_CAPTAIN {
        @Override
        public Set<EntryStatus> allowedTransitions() {
            return Set.of(APPROVED, PENDING_MANAGER, REJECTED);
        }

        @Override
        public boolean canBeEditedByCrew() {
            return false;
        }

        @Override
        public boolean canBeDeleted() {
            return false;
        }

        @Override
        public boolean isPending() {
            return true;
        }

        @Override
        public boolean isPendingCaptain() {
            return true;
        }
    },

    /**
     * PENDING_MANAGER - Waiting for Manager approval (amount > limit)
     * Manager or Captain can approve or reject
     */
    PENDING_MANAGER {
        @Override
        public Set<EntryStatus> allowedTransitions() {
            return Set.of(APPROVED, REJECTED);
        }

        @Override
        public boolean canBeEditedByCrew() {
            return false;
        }

        @Override
        public boolean canBeDeleted() {
            return false;
        }

        @Override
        public boolean isPending() {
            return true;
        }

        @Override
        public boolean isPendingManager() {
            return true;
        }
    },

    /**
     * APPROVED - Entry approved, ready for payment
     */
    APPROVED {
        @Override
        public Set<EntryStatus> allowedTransitions() {
            return Set.of(PARTIALLY_PAID, PAID);
        }

        @Override
        public boolean canBeEditedByCrew() {
            return false;
        }

        @Override
        public boolean canBeDeleted() {
            return false;
        }

        @Override
        public boolean isApproved() {
            return true;
        }
    },

    /**
     * PARTIALLY_PAID - Some payment made, not complete
     */
    PARTIALLY_PAID {
        @Override
        public Set<EntryStatus> allowedTransitions() {
            return Set.of(PAID);
        }

        @Override
        public boolean canBeEditedByCrew() {
            return false;
        }

        @Override
        public boolean canBeDeleted() {
            return false;
        }

        @Override
        public boolean isApproved() {
            return true;
        }
    },

    /**
     * PAID - Fully paid, terminal state
     */
    PAID {
        @Override
        public Set<EntryStatus> allowedTransitions() {
            return Set.of();  // Terminal
        }

        @Override
        public boolean canBeEditedByCrew() {
            return false;
        }

        @Override
        public boolean canBeEditedByCaptain() {
            return false;  // Even captain cannot edit PAID
        }

        @Override
        public boolean canBeDeleted() {
            return false;
        }

        @Override
        public boolean isFinal() {
            return true;
        }

        @Override
        public boolean isApproved() {
            return true;
        }
    },

    /**
     * REJECTED - Rejected by Captain/Manager, terminal state
     */
    REJECTED {
        @Override
        public Set<EntryStatus> allowedTransitions() {
            return Set.of();  // Terminal
        }

        @Override
        public boolean canBeEditedByCrew() {
            return false;
        }

        @Override
        public boolean canBeEditedByCaptain() {
            return false;  // Even captain cannot edit REJECTED
        }

        @Override
        public boolean canBeDeleted() {
            return false;
        }

        @Override
        public boolean isFinal() {
            return true;
        }
    };

    // ═══════════════════════════════════════════════════════════════════════════
    // ABSTRACT METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get allowed status transitions from this status.
     */
    public abstract Set<EntryStatus> allowedTransitions();

    /**
     * Check if entry can be edited by Crew in this status.
     */
    public abstract boolean canBeEditedByCrew();

    /**
     * Check if entry can be deleted in this status.
     */
    public abstract boolean canBeDeleted();

    // ═══════════════════════════════════════════════════════════════════════════
    // DEFAULT METHODS (can be overridden)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Check if entry can be edited by Captain in this status.
     * Default: Captain can edit non-final entries.
     */
    public boolean canBeEditedByCaptain() {
        return !isFinal();
    }

    /**
     * Check if transition to target status is allowed.
     */
    public boolean canTransitionTo(EntryStatus target) {
        return allowedTransitions().contains(target);
    }

    /**
     * Check if this is a terminal (final) status.
     */
    public boolean isFinal() {
        return false;
    }

    /**
     * Check if entry is pending any approval.
     */
    public boolean isPending() {
        return false;
    }

    /**
     * Check if entry is pending captain approval.
     */
    public boolean isPendingCaptain() {
        return false;
    }

    /**
     * Check if entry is pending manager approval.
     */
    public boolean isPendingManager() {
        return false;
    }

    /**
     * Check if entry is approved (includes APPROVED, PARTIALLY_PAID, PAID).
     */
    public boolean isApproved() {
        return false;
    }

    /**
     * Check if entry is payable (approved but not fully paid).
     */
    public boolean isPayable() {
        return this == APPROVED || this == PARTIALLY_PAID;
    }

    public boolean allowsPaymentReversal() { return this == PARTIALLY_PAID || this == PAID;}

    // ═══════════════════════════════════════════════════════════════════════════
    // DISPLAY HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get display name for UI.
     */
    public String getDisplayName() {
        return switch (this) {
            case DRAFT -> "Draft";
            case PENDING_CAPTAIN -> "Pending Captain";
            case PENDING_MANAGER -> "Pending Manager";
            case APPROVED -> "Approved";
            case PARTIALLY_PAID -> "Partially Paid";
            case PAID -> "Paid";
            case REJECTED -> "Rejected";
        };
    }

    /**
     * Get CSS class for UI styling.
     */
    public String getCssClass() {
        return switch (this) {
            case DRAFT -> "status-draft";
            case PENDING_CAPTAIN -> "status-pending-captain";
            case PENDING_MANAGER -> "status-pending-manager";
            case APPROVED -> "status-approved";
            case PARTIALLY_PAID -> "status-partial";
            case PAID -> "status-paid";
            case REJECTED -> "status-rejected";
        };
    }

    /**
     * Get icon for UI.
     */
    public String getIcon() {
        return switch (this) {
            case DRAFT -> "📝";
            case PENDING_CAPTAIN -> "⏳";
            case PENDING_MANAGER -> "⏳";
            case APPROVED -> "✅";
            case PARTIALLY_PAID -> "💰";
            case PAID -> "✔️";
            case REJECTED -> "❌";
        };
    }
}