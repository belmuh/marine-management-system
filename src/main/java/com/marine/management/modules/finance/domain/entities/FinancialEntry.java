package com.marine.management.modules.finance.domain.entities;

import com.marine.management.modules.finance.application.ExchangeRateService;
import com.marine.management.modules.finance.domain.enums.EntryStatus;
import com.marine.management.modules.finance.domain.enums.PaymentMethod;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.vo.EntryNumber;
import com.marine.management.modules.finance.domain.vo.Money;
import com.marine.management.shared.domain.BaseTenantEntity;
import com.marine.management.shared.exceptions.ExchangeRateCalculationException;
import jakarta.persistence.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Financial Entry - Core domain entity for expenses and incomes.
 *
 * Responsibilities:
 * - Hold entry data (amount, category, date, etc.)
 * - Manage workflow state transitions
 * - Validate business rules
 * - Calculate exchange rates
 *
 * Note: Permission checks are handled by EntryAccessPolicy in the service layer.
 */
@Entity
@Audited
@Table(
        name = "financial_entries",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_financial_entries_tenant_entry_number",
                        columnNames = {"tenant_id", "entry_number"}
                )
        },
        indexes = {
                @Index(name = "idx_financial_entries_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_financial_entries_tenant_date", columnList = "tenant_id, entry_date"),
                @Index(name = "idx_financial_entries_tenant_entry_number", columnList = "tenant_id, entry_number"),
                @Index(name = "idx_financial_entries_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_financial_entries_tenant_created_by", columnList = "tenant_id, created_by_id"),
                @Index(name = "idx_financial_entries_tenant_date_status", columnList = "tenant_id, entry_date, status"),
                @Index(name = "idx_financial_entries_category", columnList = "category_id"),
                @Index(name = "idx_financial_entries_tenant_who", columnList = "tenant_who_id"),
                @Index(name = "idx_financial_entries_tenant_main_cat", columnList = "tenant_main_category_id")
        }
)
public class FinancialEntry extends BaseTenantEntity {

    private static final Logger logger = LoggerFactory.getLogger(FinancialEntry.class);
    public static final String BASE_CURRENCY = "EUR";

    // ═══════════════════════════════════════════════════════════════════════════
    // FIELDS
    // ═══════════════════════════════════════════════════════════════════════════

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "entry_number", unique = true, nullable = false, length = 50))
    private EntryNumber entryNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EntryStatus status = EntryStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private RecordType entryType;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private FinancialCategory category;

    // 💰 AMOUNTS
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "original_amount", precision = 19, scale = 4, nullable = false)),
            @AttributeOverride(name = "currencyCode", column = @Column(name = "original_currency", length = 3, nullable = false))
    })
    private Money originalAmount;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "base_amount", precision = 19, scale = 4, nullable = false)),
            @AttributeOverride(name = "currencyCode", column = @Column(name = "base_currency", length = 3, nullable = false))
    })
    private Money baseAmount;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "approved_base_amount", precision = 19, scale = 4)),
            @AttributeOverride(name = "currencyCode", column = @Column(name = "approved_base_currency", length = 3))
    })
    private Money approvedBaseAmount;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "paid_base_amount", precision = 19, scale = 4)),
            @AttributeOverride(name = "currencyCode", column = @Column(name = "paid_base_currency", length = 3))
    })
    private Money paidBaseAmount;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @NotAudited
    @Column(length = 1000)
    private String description;

    // Context fields
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_who_id")
    private TenantWhoSelection tenantWho;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_main_category_id")
    private TenantMainCategory tenantMainCategory;

    @Column(name = "recipient", length = 50)
    private String recipient;

    @NotAudited
    @Column(name = "country", length = 50)
    private String country;

    @NotAudited
    @Column(name = "city", length = 50)
    private String city;

    @NotAudited
    @Column(name = "specific_location", length = 200)
    private String specificLocation;

    @NotAudited
    @Column(name = "vendor", length = 100)
    private String vendor;

    // Metadata fields
    @NotAudited
    @Column(name = "frequency", length = 20)
    private String frequency;

    @NotAudited
    @Column(name = "priority", length = 20)
    private String priority;

    @NotAudited
    @Column(name = "tags", length = 500)
    private String tags;

    @Column(name = "receipt_number", length = 100)
    private String receiptNumber;

    // Exchange rate
    @Column(name = "exchange_rate", precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "exchange_rate_date")
    private LocalDate exchangeRateDate;

    // Rejection
    @NotAudited
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    // Attachments
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FinancialEntryAttachment> attachments = new ArrayList<>();

    protected FinancialEntry() {}

    // ═══════════════════════════════════════════════════════════════════════════
    // FACTORY METHOD
    // ═══════════════════════════════════════════════════════════════════════════

    public static FinancialEntry create(
            EntryNumber entryNumber,
            RecordType entryType,
            FinancialCategory category,
            Money amount,
            LocalDate entryDate,
            PaymentMethod paymentMethod,
            String description,
            TenantWhoSelection tenantWho,
            TenantMainCategory tenantMainCategory,
            String recipient,
            String country,
            String city,
            String specificLocation,
            String vendor
    ) {
        Objects.requireNonNull(entryNumber, "Entry number cannot be null");
        Objects.requireNonNull(entryType, "Entry type cannot be null");
        Objects.requireNonNull(category, "Category cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(entryDate, "Entry date cannot be null");
        Objects.requireNonNull(paymentMethod, "Payment method cannot be null");

        FinancialEntry entry = new FinancialEntry();
        entry.entryNumber = entryNumber;
        entry.status = EntryStatus.DRAFT;
        entry.entryType = entryType;
        entry.category = category;
        entry.originalAmount = amount;
        entry.baseAmount = amount;
        entry.approvedBaseAmount = Money.zero(BASE_CURRENCY);
        entry.paidBaseAmount = Money.zero(BASE_CURRENCY);
        entry.entryDate = entryDate;
        entry.paymentMethod = paymentMethod;
        entry.description = description;
        entry.tenantWho = tenantWho;
        entry.tenantMainCategory = tenantMainCategory;
        entry.recipient = recipient;
        entry.country = country;
        entry.city = city;
        entry.specificLocation = specificLocation;
        entry.vendor = vendor;

        entry.validate();
        return entry;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // JPA LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════

    @PrePersist
    protected void onPrePersist() {
        if (approvedBaseAmount == null) {
            approvedBaseAmount = Money.zero(BASE_CURRENCY);
        }
        if (paidBaseAmount == null) {
            paidBaseAmount = Money.zero(BASE_CURRENCY);
        }
        validate();
    }

    @PreUpdate
    protected void onPreUpdate() {
        validate();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // WORKFLOW METHODS (Status transitions)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Submit entry for approval.
     * DRAFT → PENDING_CAPTAIN
     *
     * Note: updatedById is automatically set by AuditingEntityListener
     */
    public void submit() {
        if (this.status != EntryStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT entries can be submitted");
        }
        this.status = EntryStatus.PENDING_CAPTAIN;
    }

    /**
     * Submit directly to manager level (skipping captain).
     * Used when Captain submits own entry and manager approval is needed.
     * DRAFT → PENDING_MANAGER
     */
    public void submitToManager() {
        if (this.status != EntryStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT entries can be submitted");
        }
        this.status = EntryStatus.PENDING_MANAGER;
    }

    /**
     * Submit and auto-approve in one step.
     * Used when Captain submits own entry (no manager needed) or Admin submits.
     * DRAFT → APPROVED
     */
    public void submitAndApprove() {
        if (this.status != EntryStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT entries can be submitted");
        }
        this.status = EntryStatus.APPROVED;
        this.approvedBaseAmount = this.baseAmount;
    }

    /**
     * Approve at captain level.
     * PENDING_CAPTAIN → APPROVED or PENDING_MANAGER
     *
     * @param needsManagerApproval true if manager approval is required, false to approve directly
     * Note: updatedById is automatically set by AuditingEntityListener
     */
    public void approveByCaptain(boolean needsManagerApproval) {
        if (this.status != EntryStatus.PENDING_CAPTAIN) {
            throw new IllegalStateException("Entry is not pending captain approval");
        }
        if (needsManagerApproval) {
            this.status = EntryStatus.PENDING_MANAGER;
        } else {
            this.status = EntryStatus.APPROVED;
            this.approvedBaseAmount = this.baseAmount;
        }
    }

    /**
     * Approve at manager level.
     * PENDING_MANAGER → APPROVED
     *
     * Note: updatedById is automatically set by AuditingEntityListener
     */
    public void approveByManager() {
        if (this.status != EntryStatus.PENDING_MANAGER) {
            throw new IllegalStateException("Entry is not pending manager approval");
        }
        this.status = EntryStatus.APPROVED;
        this.approvedBaseAmount = this.baseAmount;
    }

    /**
     * Reject entry.
     * PENDING_CAPTAIN or PENDING_MANAGER → REJECTED
     *
     * @param reason the reason for rejection (required)
     * Note: updatedById is automatically set by AuditingEntityListener
     */
    public void reject(String reason) {
        if (!this.status.isPending()) {
            throw new IllegalStateException("Only pending entries can be rejected");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        this.status = EntryStatus.REJECTED;
        this.rejectionReason = reason;
    }

// ═══════════════════════════════════════════════════════════════════════════
// PAYMENT METHODS
// ═══════════════════════════════════════════════════════════════════════════

    /**
     * Record a payment for this entry.
     * Can be called multiple times for partial payments.
     *
     * @param paymentAmount the amount being paid (must be in EUR)
     * Note: updatedById is automatically set by AuditingEntityListener
     */
    public void recordPayment(Money paymentAmount) {
        Objects.requireNonNull(paymentAmount, "Payment amount cannot be null");

        if (!paymentAmount.getCurrencyCode().equals(BASE_CURRENCY)) {
            throw new IllegalArgumentException("Payment amount must be in base currency (EUR)");
        }

        if (!this.status.isApproved()) {
            throw new IllegalStateException("Cannot record payment for non-approved entry");
        }

        Money newPaidAmount = this.paidBaseAmount.add(paymentAmount);

        if (newPaidAmount.isGreaterThan(this.approvedBaseAmount)) {
            throw new IllegalArgumentException(
                    String.format("Total paid amount (%s) cannot exceed approved amount (%s)",
                            newPaidAmount, this.approvedBaseAmount)
            );
        }

        this.paidBaseAmount = newPaidAmount;

        // Auto-update status
        if (isFullyPaid()) {
            this.status = EntryStatus.PAID;
        } else {
            this.status = EntryStatus.PARTIALLY_PAID;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    public void updateDetails(
            RecordType entryType,
            FinancialCategory category,
            Money amount,
            LocalDate entryDate,
            PaymentMethod paymentMethod,
            String description
    ) {
        this.entryType = Objects.requireNonNull(entryType, "Entry type cannot be null");
        this.category = Objects.requireNonNull(category, "Category cannot be null");
        this.originalAmount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.entryDate = Objects.requireNonNull(entryDate, "Entry date cannot be null");
        this.paymentMethod = Objects.requireNonNull(paymentMethod, "Payment method cannot be null");
        this.description = description;
        validate();
    }

    public void updateContext(
            TenantWhoSelection tenantWho,
            TenantMainCategory tenantMainCategory,
            String recipient,
            String country,
            String city,
            String specificLocation,
            String vendor
    ) {
        this.tenantWho = tenantWho;
        this.tenantMainCategory = tenantMainCategory;
        this.recipient = recipient;
        this.country = country;
        this.city = city;
        this.specificLocation = specificLocation;
        this.vendor = vendor;
        validate();
    }

    public void updateMetadata(String frequency, String priority, String tags) {
        this.frequency = frequency;
        this.priority = priority;
        this.tags = tags;
    }

    public void updateReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXCHANGE RATE
    // ═══════════════════════════════════════════════════════════════════════════

    public void calculateBaseAmount(ExchangeRateService exchangeRateService) {
        Objects.requireNonNull(exchangeRateService, "Exchange rate service cannot be null");

        if (this.originalAmount.isEuro()) {
            this.baseAmount = this.originalAmount;
            this.exchangeRate = BigDecimal.ONE;
            this.exchangeRateDate = this.entryDate;
            return;
        }

        try {
            BigDecimal rate = exchangeRateService.getRate(
                    this.entryDate,
                    this.originalAmount.getCurrencyCode(),
                    BASE_CURRENCY
            );

            this.baseAmount = this.originalAmount.convertUsing(rate, BASE_CURRENCY);
            this.exchangeRate = rate;
            this.exchangeRateDate = this.entryDate;

        } catch (Exception e) {
            throw new ExchangeRateCalculationException(
                    String.format("Failed to calculate base amount for %s on %s",
                            this.originalAmount.getCurrencyCode(), this.entryDate), e);
        }
    }

    public void updateExchangeRate(BigDecimal rate, LocalDate rateDate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive");
        }
        this.exchangeRate = rate;
        this.exchangeRateDate = rateDate;
        if (originalAmount != null && !originalAmount.isEuro()) {
            this.baseAmount = this.originalAmount.convertUsing(rate, BASE_CURRENCY);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ATTACHMENT METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    public void addAttachment(FinancialEntryAttachment attachment) {
        Objects.requireNonNull(attachment, "Attachment cannot be null");
        if (attachments == null) {
            attachments = new ArrayList<>();
        }
        attachments.add(attachment);
        attachment.associateWithEntry(this);
    }

    public void removeAttachment(FinancialEntryAttachment attachment) {
        if (attachments != null && attachments.remove(attachment)) {
            attachment.dissociateFromEntry();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // QUERY METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean isFullyPaid() {
        return hasApprovedAmount() && paidBaseAmount.isGreaterThanOrEqual(approvedBaseAmount);
    }

    public boolean isPartiallyPaid() {
        return paidBaseAmount != null && !paidBaseAmount.isZero()
                && paidBaseAmount.isLessThan(approvedBaseAmount);
    }

    public boolean hasApprovedAmount() {
        return approvedBaseAmount != null && !approvedBaseAmount.isZero();
    }

    public Money getRemainingAmount() {
        if (approvedBaseAmount == null) {
            return Money.zero(BASE_CURRENCY);
        }
        return approvedBaseAmount.subtract(paidBaseAmount);
    }

    public boolean isIncome() {
        return this.entryType == RecordType.INCOME;
    }

    public boolean isExpense() {
        return this.entryType == RecordType.EXPENSE;
    }

    public boolean hasAttachments() {
        return attachments != null && !attachments.isEmpty();
    }

    public boolean isDraft() {
        return EntryStatus.DRAFT.equals(this.status);
    }

    public boolean hasWho() {
        return tenantWho != null;
    }

    public boolean hasMainCategory() {
        return tenantMainCategory != null;
    }

    public Long getWhoId() {
        return tenantWho != null && tenantWho.getWho() != null
                ? tenantWho.getWho().getId()
                : null;
    }

    public Long getMainCategoryId() {
        return tenantMainCategory != null && tenantMainCategory.getMainCategory() != null
                ? tenantMainCategory.getMainCategory().getId()
                : null;
    }

    public String getFullLocation() {
        if (country == null && city == null && specificLocation == null) {
            return null;
        }

        List<String> parts = new ArrayList<>();
        if (city != null) parts.add(city);
        if (country != null) parts.add(country);

        String location = String.join(", ", parts);
        if (specificLocation != null && !location.isEmpty()) {
            location += " - " + specificLocation;
        } else if (specificLocation != null) {
            location = specificLocation;
        }

        return location.isEmpty() ? null : location;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VALIDATION
    // ═══════════════════════════════════════════════════════════════════════════

    public void validate() {
        List<String> errors = new ArrayList<>();

        if (entryNumber == null) errors.add("Entry number is required");
        if (entryType == null) errors.add("Entry type is required");
        if (originalAmount == null) errors.add("Amount is required");
        if (originalAmount != null && originalAmount.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Amount must be positive");
        }
        if (category == null) errors.add("Category is required");
        if (category != null && !category.isActive()) errors.add("Category must be active");
        if (entryDate == null) errors.add("Entry date is required");
        if (entryDate != null && entryDate.isAfter(LocalDate.now())) {
            errors.add("Entry date cannot be in the future");
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException(String.join("; ", errors));
        }

        validateTenantConsistency();
    }

    private void validateTenantConsistency() {
        Long entryTenantId = getTenantId();

        if (entryTenantId == null) {
            return; // Not yet assigned (expected during creation)
        }

        if (tenantWho != null && tenantWho.doesNotBelongToTenant(entryTenantId)) {
            throw new SecurityException("TenantWhoSelection belongs to different tenant");
        }

        if (tenantMainCategory != null && tenantMainCategory.doesNotBelongToTenant(entryTenantId)) {
            throw new SecurityException("TenantMainCategory belongs to different tenant");
        }

        if (category != null && category.doesNotBelongToTenant(entryTenantId)) {
            throw new SecurityException("FinancialCategory belongs to different tenant");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public Object getId() {
        return id;
    }

    public UUID getEntryId() {
        return id;
    }

    public EntryNumber getEntryNumber() { return entryNumber; }
    public EntryStatus getStatus() { return status; }
    public RecordType getEntryType() { return entryType; }
    public FinancialCategory getCategory() { return category; }
    public Money getOriginalAmount() { return originalAmount; }
    public Money getBaseAmount() { return baseAmount; }
    public Money getApprovedBaseAmount() { return approvedBaseAmount; }
    public Money getPaidBaseAmount() { return paidBaseAmount; }
    public LocalDate getEntryDate() { return entryDate; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getDescription() { return description; }
    public TenantWhoSelection getTenantWho() { return tenantWho; }
    public TenantMainCategory getTenantMainCategory() { return tenantMainCategory; }
    public String getRecipient() { return recipient; }
    public String getCountry() { return country; }
    public String getCity() { return city; }
    public String getSpecificLocation() { return specificLocation; }
    public String getVendor() { return vendor; }
    public String getFrequency() { return frequency; }
    public String getPriority() { return priority; }
    public String getTags() { return tags; }
    public String getReceiptNumber() { return receiptNumber; }
    public BigDecimal getExchangeRate() { return exchangeRate; }
    public LocalDate getExchangeRateDate() { return exchangeRateDate; }
    public String getRejectionReason() { return rejectionReason; }

    public List<FinancialEntryAttachment> getAttachments() {
        return attachments != null ? List.copyOf(attachments) : List.of();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EQUALS / HASHCODE / TOSTRING
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FinancialEntry entry)) return false;
        return Objects.equals(entryNumber, entry.entryNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entryNumber);
    }

    @Override
    public String toString() {
        return String.format(
                "FinancialEntry{id=%s, number='%s', type=%s, amount=%s, status=%s}",
                id, entryNumber, entryType, originalAmount, status
        );
    }
}