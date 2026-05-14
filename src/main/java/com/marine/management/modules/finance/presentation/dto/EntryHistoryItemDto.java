package com.marine.management.modules.finance.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Unified timeline item for entry history.
 *
 * Future-proof design: supports both approval events (Faz 1)
 * and Envers revision events (Faz 3+) via the type/action pattern.
 *
 * type   → source of the event (APPROVAL, REVISION, SYSTEM)
 * action → what happened (SUBMITTED, APPROVED, REJECTED, UPDATED, etc.)
 *
 * Faz 3 migration note:
 * - details will become a sealed interface (ApprovalDetails | RevisionDetails)
 * - For now, ApprovalDetails covers all approval events with typed fields
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EntryHistoryItemDto(
        LocalDateTime timestamp,
        HistoryType type,
        HistoryAction action,
        String userName,
        String description,
        ApprovalDetails approvalDetails,
        RevisionDetails revisionDetails
) {

    /**
     * Source of the history event.
     */
    public enum HistoryType {
        APPROVAL,
        REVISION,
        SYSTEM
    }

    /**
     * Business-level action that occurred.
     */
    public enum HistoryAction {
        CREATED,
        SUBMITTED,
        APPROVED,
        PARTIALLY_APPROVED,
        REJECTED,
        RETURNED,
        UPDATED,
        PAID,
        DELETED
    }

    /**
     * Typed details for approval events.
     * All fields nullable — only relevant ones are populated per action.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ApprovalDetails(
            String approvalLevel,
            MoneyDto requestedAmount,
            MoneyDto approvedAmount,
            String reason,
            String comments
    ) {
        public static ApprovalDetails ofLevel(String approvalLevel) {
            return new ApprovalDetails(approvalLevel, null, null, null, null);
        }

        public static ApprovalDetails ofAmounts(
                String approvalLevel,
                MoneyDto requestedAmount,
                MoneyDto approvedAmount
        ) {
            return new ApprovalDetails(approvalLevel, requestedAmount, approvedAmount, null, null);
        }

        public static ApprovalDetails ofPartial(
                String approvalLevel,
                MoneyDto requestedAmount,
                MoneyDto approvedAmount,
                String comments
        ) {
            return new ApprovalDetails(approvalLevel, requestedAmount, approvedAmount, null, comments);
        }

        public static ApprovalDetails ofRejection(String approvalLevel, String reason) {
            return new ApprovalDetails(approvalLevel, null, null, reason, null);
        }
    }

    /**
     * Typed details for revision (Envers) events.
     * Contains the list of field-level changes detected between consecutive revisions.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RevisionDetails(
            int revisionNumber,
            List<FieldChange> changes
    ) {
        /**
         * A single field change between two consecutive revisions.
         */
        public record FieldChange(
                String fieldName,
                String oldValue,
                String newValue
        ) {}
    }

    // ─── Factory methods for Revision events ───

    public static EntryHistoryItemDto created(LocalDateTime timestamp, String userName) {
        return new EntryHistoryItemDto(
                timestamp,
                HistoryType.REVISION,
                HistoryAction.CREATED,
                userName,
                "Entry created",
                null,
                null
        );
    }

    public static EntryHistoryItemDto updated(
            LocalDateTime timestamp,
            String userName,
            String description,
            RevisionDetails revisionDetails
    ) {
        return new EntryHistoryItemDto(
                timestamp,
                HistoryType.REVISION,
                HistoryAction.UPDATED,
                userName,
                description,
                null,
                revisionDetails
        );
    }

    public static EntryHistoryItemDto deleted(LocalDateTime timestamp, String userName) {
        return new EntryHistoryItemDto(
                timestamp,
                HistoryType.REVISION,
                HistoryAction.DELETED,
                userName,
                "Entry deleted",
                null,
                null
        );
    }

    // ─── Factory methods for Approval events ───

    public static EntryHistoryItemDto submitted(
            LocalDateTime timestamp,
            String userName,
            String approvalLevel
    ) {
        return new EntryHistoryItemDto(
                timestamp,
                HistoryType.APPROVAL,
                HistoryAction.SUBMITTED,
                userName,
                "Submitted for " + approvalLevel.toLowerCase() + " approval",
                ApprovalDetails.ofLevel(approvalLevel),
                null
        );
    }

    public static EntryHistoryItemDto approved(
            LocalDateTime timestamp,
            String approverName,
            String approvalLevel,
            MoneyDto requestedAmount,
            MoneyDto approvedAmount
    ) {
        return new EntryHistoryItemDto(
                timestamp,
                HistoryType.APPROVAL,
                HistoryAction.APPROVED,
                approverName,
                "Approved at " + approvalLevel.toLowerCase() + " level",
                ApprovalDetails.ofAmounts(approvalLevel, requestedAmount, approvedAmount),
                null
        );
    }

    public static EntryHistoryItemDto partiallyApproved(
            LocalDateTime timestamp,
            String approverName,
            String approvalLevel,
            MoneyDto requestedAmount,
            MoneyDto approvedAmount,
            String comments
    ) {
        return new EntryHistoryItemDto(
                timestamp,
                HistoryType.APPROVAL,
                HistoryAction.PARTIALLY_APPROVED,
                approverName,
                "Partially approved at " + approvalLevel.toLowerCase() + " level",
                ApprovalDetails.ofPartial(approvalLevel, requestedAmount, approvedAmount, comments),
                null
        );
    }

    public static EntryHistoryItemDto rejected(
            LocalDateTime timestamp,
            String approverName,
            String approvalLevel,
            String reason
    ) {
        return new EntryHistoryItemDto(
                timestamp,
                HistoryType.APPROVAL,
                HistoryAction.REJECTED,
                approverName,
                "Rejected at " + approvalLevel.toLowerCase() + " level",
                ApprovalDetails.ofRejection(approvalLevel, reason),
                null
        );
    }

    public static EntryHistoryItemDto returned(
            LocalDateTime timestamp,
            String approverName,
            String approvalLevel,
            String reason
    ) {
        return new EntryHistoryItemDto(
                timestamp,
                HistoryType.APPROVAL,
                HistoryAction.RETURNED,
                approverName,
                "Returned at " + approvalLevel.toLowerCase() + " level",
                ApprovalDetails.ofRejection(approvalLevel, reason),
                null
        );
    }
}
