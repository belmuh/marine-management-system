package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.domain.entities.EntryApproval;
import com.marine.management.modules.finance.domain.entities.FinancialEntry;
import com.marine.management.modules.finance.domain.enums.ApprovalStatus;
import com.marine.management.modules.finance.infrastructure.EntryApprovalRepository;
import com.marine.management.modules.finance.infrastructure.FinancialEntryRepository;
import com.marine.management.modules.finance.presentation.dto.EntryHistoryItemDto;
import com.marine.management.modules.finance.presentation.dto.MoneyDto;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.exceptions.EntryNotFoundException;
import com.marine.management.shared.multitenant.TenantContext;
import com.marine.management.shared.security.EntryAccessPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Builds a unified timeline from multiple audit sources.
 *
 * Sources:
 * - Approval events (from EntryApproval entity) → SUBMITTED, APPROVED, REJECTED, etc.
 * - Revision events (from Hibernate Envers) → CREATED, UPDATED, DELETED
 *
 * Design decisions:
 * - Separate service (SRP): history aggregation != entry CRUD
 * - Read-only transactions: no writes, safe for performance
 * - Access control delegated to EntryAccessPolicy (same as entry read)
 * - Revision events delegate to EntryRevisionService (SRP: revision querying is separate)
 */
@Service
@Transactional(readOnly = true)
public class EntryHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(EntryHistoryService.class);

    private final FinancialEntryRepository entryRepository;
    private final EntryApprovalRepository approvalRepository;
    private final EntryRevisionService revisionService;
    private final EntryAccessPolicy accessPolicy;

    public EntryHistoryService(
            FinancialEntryRepository entryRepository,
            EntryApprovalRepository approvalRepository,
            EntryRevisionService revisionService,
            EntryAccessPolicy accessPolicy
    ) {
        this.entryRepository = entryRepository;
        this.approvalRepository = approvalRepository;
        this.revisionService = revisionService;
        this.accessPolicy = accessPolicy;
    }

    /**
     * Get unified history timeline for an entry.
     * Entry read access is required (enforced via EntryAccessPolicy).
     *
     * @param entryId the entry to get history for
     * @param currentUser the requesting user (for access check)
     * @return chronologically sorted list of history items (newest first)
     */
    public List<EntryHistoryItemDto> getHistory(UUID entryId, User currentUser) {
        FinancialEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> EntryNotFoundException.withId(entryId));

        accessPolicy.checkReadAccess(entry, currentUser);

        List<EntryHistoryItemDto> timeline = new ArrayList<>();

        // Approval events (SUBMITTED, APPROVED, REJECTED, etc.)
        timeline.addAll(buildApprovalEvents(entryId));

        // Revision events (CREATED, UPDATED, DELETED from Envers)
        timeline.addAll(revisionService.buildRevisionEvents(entryId));

        // Sort newest first for UI display
        timeline.sort(Comparator.comparing(EntryHistoryItemDto::timestamp).reversed());

        logger.debug("Built history timeline for entry {}: {} events", entryId, timeline.size());

        return timeline;
    }

    // ─── Approval event mapping ───

    private List<EntryHistoryItemDto> buildApprovalEvents(UUID entryId) {
        List<EntryApproval> approvals = approvalRepository
                .findByEntry_IdOrderByApprovalLevelAsc(entryId);

        List<EntryHistoryItemDto> events = new ArrayList<>();

        for (EntryApproval approval : approvals) {
            // Every approval starts as PENDING → that's a "submitted" event
            events.add(EntryHistoryItemDto.submitted(
                    approval.getCreatedAt(),
                    null, // submitter info not stored on approval entity
                    approval.getApprovalLevel().name()
            ));

            // If a decision was made, add the decision event
            if (approval.getApprovalStatus() != ApprovalStatus.PENDING) {
                events.add(mapApprovalDecision(approval));
            }
        }

        return events;
    }

    private EntryHistoryItemDto mapApprovalDecision(EntryApproval approval) {
        String approverName = approval.getApprover() != null
                ? approval.getApprover().getFullName()
                : null;
        String level = approval.getApprovalLevel().name();
        MoneyDto requested = MoneyDto.from(approval.getRequestedAmount());
        MoneyDto approved = MoneyDto.from(approval.getApprovedAmount());

        return switch (approval.getApprovalStatus()) {
            case APPROVED -> EntryHistoryItemDto.approved(
                    approval.getApprovalDate(),
                    approverName,
                    level,
                    requested,
                    approved
            );
            case PARTIAL -> EntryHistoryItemDto.partiallyApproved(
                    approval.getApprovalDate(),
                    approverName,
                    level,
                    requested,
                    approved,
                    approval.getRejectionReason() // used for comments too
            );
            case REJECTED -> EntryHistoryItemDto.rejected(
                    approval.getApprovalDate(),
                    approverName,
                    level,
                    approval.getRejectionReason()
            );
            case RETURNED -> EntryHistoryItemDto.returned(
                    approval.getApprovalDate(),
                    approverName,
                    level,
                    approval.getRejectionReason()
            );
            // PENDING is handled above (as "submitted" event), not a decision
            case PENDING -> throw new IllegalStateException(
                    "PENDING should not reach decision mapping: " + approval.getApprovalStatus()
            );
        };
    }
}
