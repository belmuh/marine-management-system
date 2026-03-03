package com.marine.management.modules.finance.domain.entities;

import com.marine.management.modules.finance.domain.enums.PaymentMethod;
import com.marine.management.modules.finance.domain.vo.Money;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.domain.BaseTenantEntity;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Payment history for financial entries
 * Tracks individual payment transactions (supports partial payments)
 */
@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payments_tenant", columnList = "tenant_id"),
                @Index(name = "idx_payments_entry", columnList = "entry_id"),
                @Index(name = "idx_payments_date", columnList = "payment_date"),
                @Index(name = "idx_payments_recorded_by", columnList = "recorded_by_id")
        }
)
public class Payment extends BaseTenantEntity {

    private static final Logger logger = LoggerFactory.getLogger(Payment.class);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private FinancialEntry entry;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "amount", precision = 19, scale = 4, nullable = false)),
            @AttributeOverride(name = "currencyCode", column = @Column(name = "currency", length = 3, nullable = false))
    })
    private Money amount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "notes", length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_id", nullable = false)
    private User recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    protected Payment() {}

    @Override
    public Object getId() {
        return id;
    }

    public UUID getPaymentId() {
        return id;
    }

    @PrePersist
    protected void onPrePersist() {
        if (this.recordedAt == null) {
            this.recordedAt = LocalDateTime.now();
        }
        validate();
        logger.debug("Pre-persist validation passed for payment: entry={}, amount={}",
                entry.getEntryNumber(), amount);
    }

    /**
     * Factory method to create a payment record
     */
    public static Payment create(
            FinancialEntry entry,
            Money amount,
            LocalDate paymentDate,
            String paymentReference,
            PaymentMethod paymentMethod,
            String notes,
            User recordedBy
    ) {
        Objects.requireNonNull(entry, "Entry cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(paymentDate, "Payment date cannot be null");
        Objects.requireNonNull(recordedBy, "Recorded by cannot be null");

        Payment payment = new Payment();
        payment.entry = entry;
        payment.amount = amount;
        payment.paymentDate = paymentDate;
        payment.paymentReference = paymentReference;
        payment.paymentMethod = paymentMethod;
        payment.notes = notes;
        payment.recordedBy = recordedBy;
        payment.recordedAt = LocalDateTime.now();

        payment.validate();

        logger.info("Payment created: entry={}, amount={}, date={}, by={}",
                entry.getEntryNumber(), amount, paymentDate, recordedBy.getUsername());

        return payment;
    }

    /**
     * Update payment details (reference, method, notes only)
     * Amount and date are immutable for audit trail
     */
    public void updateDetails(
            String paymentReference,
            PaymentMethod paymentMethod,
            String notes
    ) {
        this.paymentReference = paymentReference;
        this.paymentMethod = paymentMethod;
        this.notes = notes;

        logger.debug("Payment details updated: id={}, by={}", id, recordedBy.getUsername());
    }

    /**
     * Check if payment is in the future
     */
    public boolean isFuturePayment() {
        return paymentDate.isAfter(LocalDate.now());
    }

    /**
     * Check if payment is same currency as entry
     */
    public boolean isSameCurrencyAsEntry() {
        return amount.getCurrencyCode().equals(entry.getBaseAmount().getCurrencyCode());
    }

    /**
     * Validate payment business rules
     */
    private void validate() {
        if (amount == null || amount.isZero() || amount.isNegative()) {
            throw new IllegalStateException("Payment amount must be positive");
        }

        if (paymentDate == null) {
            throw new IllegalStateException("Payment date is required");
        }

        if (paymentDate.isAfter(LocalDate.now().plusDays(1))) {
            throw new IllegalStateException("Payment date cannot be more than 1 day in the future");
        }

        if (entry == null) {
            throw new IllegalStateException("Entry is required");
        }

        // Payment must be in base currency
        if (!amount.getCurrencyCode().equals(FinancialEntry.BASE_CURRENCY)) {
            throw new IllegalStateException(
                    String.format("Payment must be in base currency (%s), got %s",
                            FinancialEntry.BASE_CURRENCY, amount.getCurrencyCode())
            );
        }

        // Validate tenant consistency
        validateTenantConsistency();
    }

    /**
     * Security: Ensure payment belongs to same tenant as entry
     */
    private void validateTenantConsistency() {
        Long paymentTenantId = getTenantId();

        if (paymentTenantId == null) {
            logger.trace("Tenant ID not yet set (expected during entity creation)");
            return;
        }

        if (entry != null && entry.doesNotBelongToTenant(paymentTenantId)) {
            logger.error("SECURITY VIOLATION: Cross-tenant Payment! " +
                            "Payment tenant: {}, Entry tenant: {}",
                    paymentTenantId, entry.getTenantId());
            throw new SecurityException("Payment belongs to different tenant than entry");
        }
    }

    // Getters
    public FinancialEntry getEntry() { return entry; }
    public Money getAmount() { return amount; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public String getPaymentReference() { return paymentReference; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getNotes() { return notes; }
    public User getRecordedBy() { return recordedBy; }
    public LocalDateTime getRecordedAt() { return recordedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment payment)) return false;
        return Objects.equals(id, payment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
                "Payment{id=%s, tenantId=%s, entry=%s, amount=%s, date=%s, recordedBy=%s}",
                id, getTenantId(), entry != null ? entry.getEntryNumber() : null,
                amount, paymentDate, recordedBy != null ? recordedBy.getUsername() : null
        );
    }
}