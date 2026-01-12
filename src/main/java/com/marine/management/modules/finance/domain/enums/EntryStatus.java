package com.marine.management.modules.finance.domain.enums;

import java.util.Set;

/**
 * Financial entry lifecycle status.
 *
 * STATE MACHINE (simplified):
 * DRAFT → SUBMITTED → APPROVED → PAID
 *   ↓         ↓          ↓
 * CANCELLED CANCELLED CANCELLED
 */
public enum EntryStatus {

    DRAFT {
        @Override
        public Set<EntryStatus> allowedTransitions() {
            return Set.of(SUBMITTED, CANCELLED);
        }
    },
    SUBMITTED {
        @Override
        public Set<EntryStatus> allowedTransitions() {
            return Set.of(APPROVED, CANCELLED);
        }
    },
    APPROVED {
        @Override
        public Set<EntryStatus> allowedTransitions() {
            return Set.of(PAID, CANCELLED);
        }
    },
    PAID {
        @Override
        public Set<EntryStatus> allowedTransitions() {
            return Set.of(); // final
        }
    },
    CANCELLED {
        @Override
        public Set<EntryStatus> allowedTransitions() {
            return Set.of(); // final
        }
    };

    public abstract Set<EntryStatus> allowedTransitions();

    public boolean canTransitionTo(EntryStatus target) {
        return allowedTransitions().contains(target);
    }

    public boolean isFinal() {
        return this == PAID || this == CANCELLED;
    }
}
