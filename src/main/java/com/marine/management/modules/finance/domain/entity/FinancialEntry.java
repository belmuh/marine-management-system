package com.marine.management.modules.finance.domain.entity;

import com.marine.management.modules.finance.application.ExchangeRateService;
import com.marine.management.modules.finance.domain.enums.EntryStatus;
import com.marine.management.modules.finance.domain.enums.PaymentMethod;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.vo.EntryNumber;
import com.marine.management.modules.finance.domain.vo.Money;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.domain.BaseTenantEntity;
import com.marine.management.shared.exceptions.ExchangeRateCalculationException;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Entity
@Table(
        name = "financial_entries",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_financial_entries_entry_number", columnNames = "entry_number")
        },
        indexes = {
                @Index(name = "idx_financial_entries_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_financial_entries_tenant_date", columnList = "tenant_id, entry_date"),
                @Index(name = "idx_financial_entries_entry_number", columnList = "entry_number"),
                @Index(name = "idx_financial_entries_category", columnList = "category_id"),
                @Index(name = "idx_financial_entries_status", columnList = "status"),
                @Index(name = "idx_financial_entries_tenant_who", columnList = "tenant_who_id"),
                @Index(name = "idx_financial_entries_tenant_main_cat", columnList = "tenant_main_category_id")
        }
)
public class FinancialEntry extends BaseTenantEntity {

    private static final Logger logger = LoggerFactory.getLogger(FinancialEntry.class);

    public static final String BASE_CURRENCY = "EUR";

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private FinancialCategory category;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "original_amount", precision = 19, scale = 4)),
            @AttributeOverride(name = "currencyCode", column = @Column(name = "original_currency", length = 3))
    })
    private Money originalAmount;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "base_amount", precision = 19, scale = 4)),
            @AttributeOverride(name = "currencyCode", column = @Column(name = "base_currency", length = 3))
    })
    private Money baseAmount;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_who_id")
    private TenantWhoSelection tenantWho;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_main_category_id")
    private TenantMainCategory tenantMainCategory;

    @Column(name = "recipient", length = 50)
    private String recipient;

    @Column(name = "country", length = 50)
    private String country;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "specific_location", length = 200)
    private String specificLocation;

    @Column(name = "vendor", length = 100)
    private String vendor;

    @Column(name = "frequency", length = 20)
    private String frequency;

    @Column(name = "priority", length = 20)
    private String priority;

    @Column(name = "tags", length = 500)
    private String tags;

    @Column(name = "receipt_number", length = 100)
    private String receiptNumber;

    @Column(name = "exchange_rate", precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "exchange_rate_date")
    private LocalDate exchangeRateDate;

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FinancialEntryAttachment> attachments = new ArrayList<>();

    // Removed: createdBy, createdAt, updatedBy, updatedAt, version (from BaseAuditedEntity)

    protected FinancialEntry() {}

    @Override
    public Object getId() {
        return id;
    }

    public UUID getEntryId() {
        return id;
    }

    @PrePersist
    protected void onPrePersist() {
        validate();
        logger.debug("Pre-persist validation passed for entry: {}", entryNumber);
    }

    @PreUpdate
    protected void onPreUpdate() {
        validate();
        logger.trace("Pre-update validation passed for entry: {}", entryNumber);
    }

    public static FinancialEntry create(
            EntryNumber entryNumber,
            RecordType entryType,
            FinancialCategory category,
            Money amount,
            LocalDate entryDate,
            PaymentMethod paymentMethod,
            String description,
            User creator,
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
        Objects.requireNonNull(creator, "Creator cannot be null");

        FinancialEntry entry = new FinancialEntry();
        entry.entryNumber = entryNumber;
        entry.status = EntryStatus.DRAFT;
        entry.entryType = entryType;
        entry.category = category;
        entry.originalAmount = amount;
        entry.baseAmount = amount;
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
                            this.originalAmount.getCurrencyCode(),
                            this.entryDate),
                    e
            );
        }
    }

    public void recalculateBaseAmount(ExchangeRateService exchangeRateService, User updater) {
        requireEditPermission(updater);
        calculateBaseAmount(exchangeRateService);
    }

    public void updateDetails(
            RecordType entryType,
            FinancialCategory category,
            Money amount,
            LocalDate entryDate,
            PaymentMethod paymentMethod,
            String description,
            User updater
    ) {
        requireEditPermission(updater);

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
            String vendor,
            User updater
    ) {
        requireEditPermission(updater);

        this.tenantWho = tenantWho;
        this.tenantMainCategory = tenantMainCategory;
        this.recipient = recipient;
        this.country = country;
        this.city = city;
        this.specificLocation = specificLocation;
        this.vendor = vendor;

        validate();
    }

    public void updateMetadata(String frequency, String priority, String tags, User updater) {
        requireEditPermission(updater);
        this.frequency = frequency;
        this.priority = priority;
        this.tags = tags;
    }

    public void updateReceiptNumber(String receiptNumber, User updater) {
        requireEditPermission(updater);
        this.receiptNumber = receiptNumber;
    }

    public void updateExchangeRate(BigDecimal rate, LocalDate rateDate, User updater) {
        requireEditPermission(updater);

        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive");
        }

        this.exchangeRate = rate;
        this.exchangeRateDate = rateDate;

        if (originalAmount != null && !originalAmount.isEuro()) {
            this.baseAmount = this.originalAmount.convertUsing(rate, BASE_CURRENCY);
        }
    }

    public void changeStatus(EntryStatus newStatus, User updater) {
        requireEditPermission(updater);

        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Invalid status transition: " + this.status + " → " + newStatus
            );
        }

        EntryStatus oldStatus = this.status;
        this.status = newStatus;

        logger.info(
                "Entry status changed: id={}, from={}, to={}, by={}",
                id, oldStatus, newStatus, updater.getUsername()
        );
    }

    public void addAttachment(FinancialEntryAttachment attachment, User updater) {
        requireEditPermission(updater);
        Objects.requireNonNull(attachment, "Attachment cannot be null");

        if (attachments == null) {
            attachments = new ArrayList<>();
        }
        attachments.add(attachment);
        attachment.associateWithEntry(this);
    }

    public void removeAttachment(FinancialEntryAttachment attachment, User updater) {
        requireEditPermission(updater);

        if (attachments != null && attachments.remove(attachment)) {
            attachment.dissociateFromEntry();
        }
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

    public boolean isTechnicalExpense() {
        return isExpense() && category != null && category.isTechnical();
    }

    public boolean hasWho() {
        return tenantWho != null;
    }

    public boolean hasMainCategory() {
        return tenantMainCategory != null;
    }

    public boolean isDetailedEntry() {
        return hasWho() && hasMainCategory();
    }

    public boolean isDraft() {
        return EntryStatus.DRAFT.equals(this.status);
    }

    public void canBeEditedBy(User user) {
        requireEditPermission(user);
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
        validateCategoryMapping();
    }

    private void validateTenantConsistency() {
        Long entryTenantId = getTenantId();

        if (entryTenantId == null) {
            logger.trace("Tenant ID not yet set (expected during entity creation)");
            return;
        }

        if (tenantWho != null) {
            if (tenantWho.doesNotBelongToTenant(entryTenantId)) {
                logger.error("SECURITY VIOLATION: Cross-tenant TenantWhoSelection! " +
                                "Entry tenant: {}, WHO tenant: {}",
                        entryTenantId, tenantWho.getTenantId());
                throw new SecurityException("TenantWhoSelection belongs to different tenant.");
            }
        }

        if (tenantMainCategory != null) {
            if (tenantMainCategory.doesNotBelongToTenant(entryTenantId)) {
                logger.error("SECURITY VIOLATION: Cross-tenant TenantMainCategory! " +
                                "Entry tenant: {}, MainCategory tenant: {}",
                        entryTenantId, tenantMainCategory.getTenantId());
                throw new SecurityException("TenantMainCategory belongs to different tenant.");
            }
        }

        if (category != null) {
            if (category.doesNotBelongToTenant(entryTenantId)) {
                logger.error("SECURITY VIOLATION: Cross-tenant FinancialCategory! " +
                                "Entry tenant: {}, Category tenant: {}",
                        entryTenantId, category.getTenantId());
                throw new SecurityException("FinancialCategory belongs to different tenant.");
            }
        }
    }

    private void validateCategoryMapping() {
        if (tenantMainCategory != null && category != null) {
            if (logger.isTraceEnabled()) {
                logger.trace("Category mapping: MainCategory={}, FinancialCategory={}",
                        tenantMainCategory.getMainCategory().getCode(),
                        category.getCode());
            }
        }
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

    private void requireEditPermission(User user) {
        if (doesNotBelongToTenant(user.getOrganizationId())) {
            logger.error("SECURITY VIOLATION: Cross-tenant access for FinancialEntry! " +
                            "Entry tenant: {}, User tenant: {}, User: {}",
                    getTenantId(),
                    user.getOrganizationId(),
                    user.getUsername());
            throw new SecurityException("Cross-tenant access attempt detected for FinancialEntry.");
        }

        UUID createdById = getCreatedById();
        if (createdById != null && !createdById.equals(user.getUserId()) && !user.canEditAnyEntry()) {
            throw new SecurityException(
                    String.format("User '%s' does not have permission to edit this entry",
                            user.getUsername())
            );
        }
    }

    // Getters
    public EntryNumber getEntryNumber() { return entryNumber; }
    public EntryStatus getStatus() { return status; }
    public RecordType getEntryType() { return entryType; }
    public FinancialCategory getCategory() { return category; }
    public Money getOriginalAmount() { return originalAmount; }
    public Money getBaseAmount() { return baseAmount; }
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
    public List<FinancialEntryAttachment> getAttachments() {
        return attachments != null ? List.copyOf(attachments) : List.of();
    }

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
                "FinancialEntry{id=%s, tenantId=%s, number='%s', type=%s, amount=%s, status=%s}",
                id, getTenantId(), entryNumber, entryType, originalAmount, status
        );
    }
}