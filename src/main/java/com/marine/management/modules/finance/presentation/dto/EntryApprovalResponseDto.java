package com.marine.management.modules.finance.presentation.dto;

import com.marine.management.modules.finance.domain.entities.EntryApproval;
import com.marine.management.modules.finance.domain.enums.ApprovalLevel;
import com.marine.management.modules.finance.domain.enums.ApprovalStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for entry approval history
 */
public record EntryApprovalResponseDto(
        UUID approvalId,
        UUID entryId,
        ApprovalLevel approvalLevel,
        MoneyDto requestedAmount,
        MoneyDto approvedAmount,
        ApprovalStatus approvalStatus,
        String rejectionReason,
        UUID approverId,
        String approverName,
        LocalDateTime approvalDate,
        LocalDateTime createdAt
) {

    public static EntryApprovalResponseDto from(EntryApproval approval) {
        return new EntryApprovalResponseDto(
                approval.getApprovalId(),
                approval.getEntry().getEntryId(),
                approval.getApprovalLevel(),
                MoneyDto.from(approval.getRequestedAmount()),
                approval.getApprovedAmount() != null ? MoneyDto.from(approval.getApprovedAmount()) : null,
                approval.getApprovalStatus(),
                approval.getRejectionReason(),
                approval.getApprover() != null ? approval.getApprover().getUserId() : null,
                approval.getApprover() != null ? approval.getApprover().getFullName() : null,
                approval.getApprovalDate(),
                approval.getCreatedAt()
        );
    }
}