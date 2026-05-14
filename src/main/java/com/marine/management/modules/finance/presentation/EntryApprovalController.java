package com.marine.management.modules.finance.presentation;

import com.marine.management.modules.finance.application.ApprovalService;
import com.marine.management.modules.finance.domain.vo.Money;
import com.marine.management.modules.finance.presentation.dto.EntryApprovalResponseDto;
import com.marine.management.modules.finance.presentation.dto.EntryResponseDto;
import com.marine.management.modules.users.domain.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Controller for approval workflow operations.
 *
 * Endpoints:
 * - GET /pending: Get pending items for current user
 * - POST /{id}/submit: Submit for approval (DRAFT → PENDING_CAPTAIN)
 * - POST /{id}/approve: Approve at current level
 * - POST /{id}/reject: Reject entry
 * - POST /bulk/approve: Bulk approve
 */
@RestController
@RequestMapping("/api/finance/entries")
public class EntryApprovalController {

    private final ApprovalService approvalService;

    public EntryApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // APPROVAL HISTORY
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/finance/entries/{id}/approvals
     * Returns the full approval history for a given entry.
     */
    @GetMapping("/{id}/approvals")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EntryApprovalResponseDto>> getApprovalHistory(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(approvalService.getApprovalHistory(id, currentUser));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PENDING LIST
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get pending items for current user.
     * - CREW: Own DRAFT and REJECTED entries
     * - MANAGER: PENDING_MANAGER entries
     * - CAPTAIN: PENDING_CAPTAIN + PENDING_MANAGER entries
     */
    @GetMapping("/pending")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EntryResponseDto>> getPendingForUser(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(approvalService.getPendingForUser(currentUser));
    }

    /**
     * Get pending count for badge display.
     */
    @GetMapping("/pending/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Long> getPendingCount(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(approvalService.getPendingCountForUser(currentUser));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SUBMIT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Submit entry for approval.
     * DRAFT → PENDING_CAPTAIN
     */
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('ENTRY_SUBMIT')")
    public ResponseEntity<EntryResponseDto> submitForApproval(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(approvalService.submit(id, currentUser));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // APPROVE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Approve entry at current level (auto-detect).
     *
     * Body is optional. Omit (or set approvedAmount = null) for full approval.
     * For partial approval at MANAGER level, supply approvedAmount + currency (EUR).
     * Partial amount is silently ignored at CAPTAIN level (captain always approves full or escalates).
     *
     * Example — full: POST /api/finance/entries/{id}/approve  (no body)
     * Example — partial: POST /api/finance/entries/{id}/approve
     *   { "approvedAmount": "350.00", "currency": "EUR" }
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('ENTRY_APPROVE_CAPTAIN', 'ENTRY_APPROVE_MANAGER')")
    public ResponseEntity<EntryResponseDto> approve(
            @PathVariable UUID id,
            @RequestBody(required = false) ApproveRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        Money approvedAmount = (request != null && request.approvedAmount() != null)
                ? Money.of(request.approvedAmount(), request.currency() != null ? request.currency() : "EUR")
                : null;
        return ResponseEntity.ok(approvalService.approve(id, currentUser, approvedAmount));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REJECT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Reject entry with reason.
     * PENDING_CAPTAIN or PENDING_MANAGER → REJECTED
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('ENTRY_REJECT')")
    public ResponseEntity<EntryResponseDto> reject(
            @PathVariable UUID id,
            @Valid @RequestBody RejectRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(approvalService.reject(id, request.reason(), currentUser));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BULK OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Bulk approve entries.
     */
    @PostMapping("/bulk/approve")
    @PreAuthorize("hasAnyAuthority('ENTRY_APPROVE_CAPTAIN', 'ENTRY_APPROVE_MANAGER')")
    public ResponseEntity<ApprovalService.BulkResult> bulkApprove(
            @Valid @RequestBody BulkApproveRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(approvalService.bulkApprove(request.entryIds(), currentUser));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DTOs
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Optional body for approve endpoint.
     * Omit entirely (or leave approvedAmount null) for full approval.
     * Supply approvedAmount (and optional currency, default EUR) for partial approval at manager level.
     */
    public record ApproveRequest(
            @DecimalMin(value = "0.01", message = "Approved amount must be greater than zero")
            BigDecimal approvedAmount,

            String currency   // defaults to EUR if omitted
    ) {}

    public record RejectRequest(
            @NotBlank(message = "Rejection reason is required")
            String reason
    ) {}

    public record BulkApproveRequest(
            List<UUID> entryIds
    ) {}
}