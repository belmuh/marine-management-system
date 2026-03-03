package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.domain.entities.FinancialEntry;
import com.marine.management.modules.finance.domain.enums.EntryStatus;
import com.marine.management.modules.finance.infrastructure.FinancialEntryRepository;
import com.marine.management.modules.finance.presentation.dto.EntryResponseDto;
import com.marine.management.modules.organization.domain.Organization;
import com.marine.management.modules.users.domain.User;
import com.marine.management.modules.users.infrastructure.UserRepository;
import com.marine.management.shared.exceptions.EntryNotFoundException;
import com.marine.management.shared.multitenant.TenantContext;
import com.marine.management.shared.security.EntryAccessPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for approval workflow operations.
 *
 * Approval Flow:
 * 1. Crew submits entry → DRAFT → PENDING_CAPTAIN
 * 2. Captain approves:
 *    - If amount > limit AND managerApprovalEnabled → PENDING_MANAGER
 *    - Otherwise → APPROVED
 * 3. Manager approves (or Captain) → APPROVED
 *
 * Captain is "super approver" - can approve at any level.
 * All approvals are logged via Envers audit trail.
 */
@Service
@Transactional(readOnly = true)
public class ApprovalService {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalService.class);

    private final FinancialEntryRepository entryRepository;
    private final EntryAccessPolicy accessPolicy;
    private final UserRepository userRepository;

    public ApprovalService(
            FinancialEntryRepository entryRepository,
            EntryAccessPolicy accessPolicy,
            UserRepository userRepository
    ) {
        this.entryRepository = entryRepository;
        this.accessPolicy = accessPolicy;
        this.userRepository = userRepository;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PENDING LISTS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get pending items for user based on their role.
     * - CREW: Own DRAFT and REJECTED
     * - MANAGER: PENDING_MANAGER
     * - CAPTAIN: PENDING_CAPTAIN + PENDING_MANAGER
     */
    public List<EntryResponseDto> getPendingForUser(User user) {
        guardTenantContext();
        verifyUserBelongsToCurrentTenant(user);

        Specification<FinancialEntry> spec = accessPolicy.getPendingSpecification(user);
        List<FinancialEntry> entries = entryRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "entryDate"));

        // 🆕 Batch user lookup — tek query ile tüm user adlarını çek
        Set<UUID> creatorIds = entries.stream()
                .map(FinancialEntry::getCreatedById)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, String> userNameMap = userRepository.findNamesByIds(creatorIds);

        return entries.stream()
                .map(entry -> {
                    UUID creatorId = entry.getCreatedById();

                    String createdByName =
                            creatorId == null
                                    ? "Unknown"
                                    : userNameMap.getOrDefault(creatorId, "Unknown");

                    return EntryResponseDto.fromWithUser(entry, createdByName);
                })
                .toList();
    }

    /**
     * Get count of pending items for user (for dashboard badge).
     */
    public long getPendingCountForUser(User user) {
        guardTenantContext();
        verifyUserBelongsToCurrentTenant(user);

        Specification<FinancialEntry> spec = accessPolicy.getPendingSpecification(user);
        return entryRepository.count(spec);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SUBMIT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Submit entry for approval.
     * DRAFT → PENDING_CAPTAIN
     */
    @Transactional
    public EntryResponseDto submit(UUID entryId, User submitter) {
        guardTenantContext();
        verifyUserBelongsToCurrentTenant(submitter);

        FinancialEntry entry = findEntryOrThrow(entryId);
        accessPolicy.checkSubmitAccess(entry, submitter);

        routeByRole(entry, submitter);

        logger.info("Entry submitted for approval: id={}, by={}", entryId, submitter.getEmail());

        return EntryResponseDto.fromWithUser(entry, submitter.getFullName());
    }


    /**
     * Route entry based on submitter's role and tenant configuration.
     *
     * - CREW/MANAGER submit → PENDING_CAPTAIN (always needs captain review)
     * - CAPTAIN submit → skip own level:
     *     - If managerApprovalEnabled AND amount > limit → PENDING_MANAGER
     *     - Otherwise → APPROVED (captain is the final authority)
     * - ADMIN submit → APPROVED (admin bypass)
     */
    private void routeByRole(FinancialEntry entry, User submitter) {
        switch (submitter.getRoleEnum()) {
            case CAPTAIN -> submitAsCaptain(entry, submitter);
            case ADMIN -> submitAsAdmin(entry);
            default -> entry.submit();  // CREW, MANAGER → PENDING_CAPTAIN
        }
    }

    private void submitAsCaptain(FinancialEntry entry, User captain) {
        // Captain submitting own entry — skip PENDING_CAPTAIN
        Organization tenant = captain.getOrganization();
        boolean needsManager = isManagerApprovalRequired(entry, tenant);

        if (needsManager) {
            entry.submitToManager();  // 🆕 entity method gerekli
        } else {
            entry.submitAndApprove(); // 🆕 entity method gerekli
        }
    }

    private void submitAsAdmin(FinancialEntry entry) {
        entry.submitAndApprove();
    }

// ═══════════════════════════════════════════════════════════════════════════
// APPROVE - CAPTAIN LEVEL
// ═══════════════════════════════════════════════════════════════════════════

    /**
     * Captain approves entry at captain level.
     *
     * Logic:
     * - If managerApprovalEnabled AND amount > limit → PENDING_MANAGER
     * - Otherwise → APPROVED (and sets approvedBaseAmount)
     */
    @Transactional
    public EntryResponseDto approveByCaptain(UUID entryId, User captain) {
        guardTenantContext();
        verifyUserBelongsToCurrentTenant(captain);

        FinancialEntry entry = findEntryOrThrow(entryId);

        // Validate: must be PENDING_CAPTAIN
        if (entry.getStatus() != EntryStatus.PENDING_CAPTAIN) {
            throw new IllegalStateException("Entry is not pending captain approval");
        }

        accessPolicy.checkApproveAccess(entry, captain);

        // Check if manager approval is needed
        Organization tenant = captain.getOrganization();
        boolean needsManagerApproval = isManagerApprovalRequired(entry, tenant);

        // Entity handles status transition and approvedBaseAmount setting
        entry.approveByCaptain(needsManagerApproval); // ✅ User parameter removed

        if (needsManagerApproval) {
            logger.info("Entry approved by captain, sent to manager: id={}, captain={}, amount={}",
                    entryId, captain.getEmail(), entry.getBaseAmount());
        } else {
            logger.info("Entry fully approved by captain: id={}, captain={}",
                    entryId, captain.getEmail());
        }

        return EntryResponseDto.fromWithUser(entry, captain.getFullName());
    }

// ═══════════════════════════════════════════════════════════════════════════
// APPROVE - MANAGER LEVEL
// ═══════════════════════════════════════════════════════════════════════════

    /**
     * Manager (or Captain) approves entry at manager level.
     * PENDING_MANAGER → APPROVED (and sets approvedBaseAmount)
     */
    @Transactional
    public EntryResponseDto approveByManager(UUID entryId, User approver) {
        guardTenantContext();
        verifyUserBelongsToCurrentTenant(approver);

        FinancialEntry entry = findEntryOrThrow(entryId);

        // Validate: must be PENDING_MANAGER
        if (entry.getStatus() != EntryStatus.PENDING_MANAGER) {
            throw new IllegalStateException("Entry is not pending manager approval");
        }

        accessPolicy.checkApproveAccess(entry, approver);

        // Entity handles status transition and approvedBaseAmount setting
        entry.approveByManager(); // ✅ User parameter removed

        logger.info("Entry approved at manager level: id={}, approver={}, role={}",
                entryId, approver.getEmail(), approver.getRoleEnum());

        return EntryResponseDto.fromWithUser(entry, approver.getFullName());
    }

// ═══════════════════════════════════════════════════════════════════════════
// APPROVE - ANY LEVEL (convenience method)
// ═══════════════════════════════════════════════════════════════════════════

    /**
     * Approve entry at current level (Captain or Manager).
     * Automatically determines the correct approval action.
     */
    @Transactional
    public EntryResponseDto approve(UUID entryId, User approver) {
        guardTenantContext();
        verifyUserBelongsToCurrentTenant(approver);

        FinancialEntry entry = findEntryOrThrow(entryId);

        return switch (entry.getStatus()) {
            case PENDING_CAPTAIN -> approveByCaptain(entryId, approver);
            case PENDING_MANAGER -> approveByManager(entryId, approver);
            default -> throw new IllegalStateException("Entry is not pending approval");
        };
    }

// ═══════════════════════════════════════════════════════════════════════════
// REJECT
// ═══════════════════════════════════════════════════════════════════════════

    /**
     * Reject entry (at any pending level).
     * PENDING_CAPTAIN or PENDING_MANAGER → REJECTED
     */
    @Transactional
    public EntryResponseDto reject(UUID entryId, String reason, User rejector) {
        guardTenantContext();
        verifyUserBelongsToCurrentTenant(rejector);

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }

        FinancialEntry entry = findEntryOrThrow(entryId);
        accessPolicy.checkRejectAccess(entry, rejector);

        // Entity handles status transition and rejection reason
        entry.reject(reason); // ✅ User parameter removed

        logger.info("Entry rejected: id={}, rejector={}, reason='{}'",
                entryId, rejector.getEmail(), reason);

        return EntryResponseDto.fromWithUser(entry, rejector.getFullName());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BULK OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Bulk approve entries.
     */
    @Transactional
    public BulkResult bulkApprove(List<UUID> entryIds, User approver) {
        int success = 0;
        int failed = 0;

        for (UUID id : entryIds) {
            try {
                approve(id, approver);
                success++;
            } catch (Exception e) {
                logger.warn("Bulk approve failed for entry {}: {}", id, e.getMessage());
                failed++;
            }
        }

        return new BulkResult(success, failed);
    }

    public record BulkResult(int success, int failed) {}

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Check if manager approval is required based on tenant settings and amount.
     */
    private boolean isManagerApprovalRequired(FinancialEntry entry, Organization tenant) {
        // If manager approval is not enabled, skip
        if (!tenant.isManagerApprovalEnabled()) {
            return false;
        }

        // If no limit set, no manager approval needed
        BigDecimal limit = tenant.getApprovalLimit();
        if (limit == null) {
            return false;
        }

        // Compare entry amount with limit
        BigDecimal entryAmount = entry.getBaseAmount().getAmount();
        return entryAmount.compareTo(limit) > 0;
    }

    private void guardTenantContext() {
        if (!TenantContext.hasTenantContext()) {
            throw new AccessDeniedException("No tenant context available");
        }
    }

    private void verifyUserBelongsToCurrentTenant(User user) {
        Long currentTenantId = TenantContext.getCurrentTenantId();
        Long userTenantId = user.getOrganization().getOrganizationId();

        if (!currentTenantId.equals(userTenantId)) {
            throw new AccessDeniedException("User does not belong to current tenant");
        }
    }

    private FinancialEntry findEntryOrThrow(UUID id) {
        return entryRepository.findById(id)
                .orElseThrow(() -> EntryNotFoundException.withId(id));
    }
}