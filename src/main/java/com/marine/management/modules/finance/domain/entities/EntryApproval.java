package com.marine.management.modules.finance.domain.entities;

import com.marine.management.modules.finance.domain.enums.ApprovalLevel;
import com.marine.management.modules.finance.domain.enums.ApprovalStatus;
import com.marine.management.modules.finance.domain.vo.Money;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.domain.BaseTenantEntity;  // 👈 CHANGED
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Audit trail for entry approval chain
 * Tracks each approval level decision (Captain, Manager)
 */
@Entity
@Table(
        name = "entry_approvals",
        indexes = {
                @Index(name = "idx_entry_approvals_tenant", columnList = "tenant_id"),  // 👈 NEW
                @Index(name = "idx_entry_approvals_entry", columnList = "entry_id"),
                @Index(name = "idx_entry_approvals_level_status", columnList = "approval_level, approval_status"),
                @Index(name = "idx_entry_approvals_approver", columnList = "approver_id")
        }
)
public class EntryApproval extends BaseTenantEntity {  // 👈 CHANGED from BaseAuditedEntity

    private static final Logger logger = LoggerFactory.getLogger(EntryApproval.class);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private FinancialEntry entry;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_level", nullable = false, length = 20)
    private ApprovalLevel approvalLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    // Amount requested at this approval level (base currency)
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "requested_amount", precision = 19, scale = 4, nullable = false)),
            @AttributeOverride(name = "currencyCode", column = @Column(name = "requested_currency", length = 3, nullable = false))
    })
    private Money requestedAmount;

    // Amount approved at this approval level (base currency)
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "approved_amount", precision = 19, scale = 4)),
            @AttributeOverride(name = "currencyCode", column = @Column(name = "approved_currency", length = 3))
    })
    private Money approvedAmount;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private User approver;

    @Column(name = "approval_date")
    private LocalDateTime approvalDate;

    protected EntryApproval() {}

    @Override
    public Object getId() {
        return id;
    }

    public UUID getApprovalId() {
        return id;
    }

    @PrePersist
    protected void onPrePersist() {
        validate();
        logger.debug("Pre-persist validation passed for approval: entry={}, level={}",
                entry.getEntryNumber(), approvalLevel);
    }

    /**
     * Factory method to create a pending approval
     */
    public static EntryApproval createPending(
            FinancialEntry entry,
            ApprovalLevel level,
            Money requestedAmount
    ) {
        Objects.requireNonNull(entry, "Entry cannot be null");
        Objects.requireNonNull(level, "Approval level cannot be null");
        Objects.requireNonNull(requestedAmount, "Requested amount cannot be null");

        EntryApproval approval = new EntryApproval();
        approval.entry = entry;
        approval.approvalLevel = level;
        approval.approvalStatus = ApprovalStatus.PENDING;
        approval.requestedAmount = requestedAmount;

        logger.debug("Created pending approval: entry={}, level={}, amount={}",
                entry.getEntryNumber(), level, requestedAmount);

        return approval;
    }

    /**
     * Approve with full requested amount
     */
    public void approveFullAmount(User approver) {
        approve(this.requestedAmount, approver, null);
    }

    /**
     * Approve with partial amount
     */
    public void approvePartialAmount(Money approvedAmount, User approver, String comments) {
        if (approvedAmount.isGreaterThan(this.requestedAmount)) {
            throw new IllegalArgumentException(
                    String.format("Approved amount (%s) cannot exceed requested amount (%s)",
                            approvedAmount, this.requestedAmount)
            );
        }
        approve(approvedAmount, approver, comments);
    }

    /**
     * Reject the approval
     */
    public void reject(User rejector, String reason) {
        Objects.requireNonNull(rejector, "Rejector cannot be null");
        Objects.requireNonNull(reason, "Rejection reason cannot be null");

        if (this.approvalStatus != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Can only reject pending approvals");
        }

        this.approvalStatus = ApprovalStatus.REJECTED;
        this.rejectionReason = reason;
        this.approver = rejector;
        this.approvalDate = LocalDateTime.now();

        logger.info("Approval rejected: entry={}, level={}, by={}, reason={}",
                entry.getEntryNumber(), approvalLevel, rejector.getUsername(), reason);
    }

    /**
     * Check if this is a partial approval
     */
    public boolean isPartialApproval() {
        return approvedAmount != null
                && approvedAmount.isLessThan(requestedAmount);
    }

    /**
     * Check if this is a full approval
     */
    public boolean isFullApproval() {
        return approvedAmount != null
                && approvedAmount.isGreaterThanOrEqual(requestedAmount);
    }

    /**
     * Get reduction amount (requested - approved)
     */
    public Money getReductionAmount() {
        if (approvedAmount == null) {
            return Money.zero(requestedAmount.getCurrencyCode());
        }
        return requestedAmount.subtract(approvedAmount);
    }

    /**
     * Get reduction percentage
     */
    public double getReductionPercentage() {
        if (approvedAmount == null || requestedAmount.isZero()) {
            return 0.0;
        }
        Money reduction = getReductionAmount();
        return reduction.getAmount()
                .divide(requestedAmount.getAmount(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(new java.math.BigDecimal("100"))
                .doubleValue();
    }

    private void approve(Money amount, User approver, String comments) {
        Objects.requireNonNull(amount, "Approved amount cannot be null");
        Objects.requireNonNull(approver, "Approver cannot be null");

        if (this.approvalStatus != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Can only approve pending approvals");
        }

        boolean isPartial = amount.isLessThan(this.requestedAmount);

        this.approvedAmount = amount;
        this.approvalStatus = isPartial ? ApprovalStatus.PARTIAL : ApprovalStatus.APPROVED;
        this.approver = approver;
        this.approvalDate = LocalDateTime.now();
        this.rejectionReason = comments; // Use rejection_reason column for comments too

        logger.info("Approval completed: entry={}, level={}, requested={}, approved={}, status={}, by={}",
                entry.getEntryNumber(), approvalLevel, requestedAmount, approvedAmount,
                approvalStatus, approver.getUsername());
    }

    // 👈 NEW: Tenant consistency validation
    private void validate() {
        Long approvalTenantId = getTenantId();

        if (approvalTenantId == null) {
            logger.trace("Tenant ID not yet set (expected during entity creation)");
            return;
        }

        if (entry != null && entry.doesNotBelongToTenant(approvalTenantId)) {
            logger.error("SECURITY VIOLATION: Cross-tenant EntryApproval! " +
                            "Approval tenant: {}, Entry tenant: {}",
                    approvalTenantId, entry.getTenantId());
            throw new SecurityException("EntryApproval belongs to different tenant than entry");
        }
    }

    // Getters
    public FinancialEntry getEntry() { return entry; }
    public ApprovalLevel getApprovalLevel() { return approvalLevel; }
    public ApprovalStatus getApprovalStatus() { return approvalStatus; }
    public Money getRequestedAmount() { return requestedAmount; }
    public Money getApprovedAmount() { return approvedAmount; }
    public String getRejectionReason() { return rejectionReason; }
    public User getApprover() { return approver; }
    public LocalDateTime getApprovalDate() { return approvalDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntryApproval that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
                "EntryApproval{id=%s, tenantId=%s, entry=%s, level=%s, status=%s, requested=%s, approved=%s}",
                id, getTenantId(), entry.getEntryNumber(), approvalLevel, approvalStatus, requestedAmount, approvedAmount
        );
    }
}