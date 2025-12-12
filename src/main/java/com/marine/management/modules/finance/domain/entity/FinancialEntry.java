package com.marine.management.modules.finance.domain.entity;

import com.marine.management.modules.finance.domain.vo.EntryNumber;
import com.marine.management.modules.finance.domain.enums.PaymentMethod;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.vo.Money;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.kernel.security.Role;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "financial_entries")
public class FinancialEntry {

    public static final String BASE_CURRENCY = "EUR";

    // === IDENTITY ===
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "entry_number", unique = true, nullable = false))
    private EntryNumber entryNumber;

    // === CORE ATTRIBUTES ===
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
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

    @Column(nullable = false)
    private LocalDate entryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(length = 1000)
    private String description;

    // === CONTEXTUAL DATA ===
    @Column(name = "who_id")
    private Long whoId;  // Reference to Who entity (optional)

    @Column(name = "main_category_id")
    private Long mainCategoryId;  // Reference to MainCategory entity (optional)

    @Column(name = "recipient", length = 50)
    private String recipient; // For expense: "Crew", "Main Yacht" | For income: "Source"

    @Column(name = "country", length = 50)
    private String country;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "specific_location", length = 200)
    private String specificLocation;

    @Column(name = "vendor", length = 100)
    private String vendor;

    // === METADATA (Optional) ===
    @Column(name = "frequency", length = 20)
    private String frequency;

    @Column(name = "priority", length = 20)
    private String priority;

    @Column(name = "tags", length = 500)
    private String tags;

    // === FINANCIAL DETAILS ===
    @Column(name = "receipt_number", length = 100)
    private String receiptNumber;

    @Column(name = "exchange_rate", precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "exchange_rate_date")
    private LocalDate exchangeRateDate;

    // === AGGREGATE RELATIONS ===
    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FinancialEntryAttachment> attachments = new ArrayList<>();

    // === AUDIT ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    // === CONSTRUCTORS ===
    protected FinancialEntry() {
        // JPA
    }

    public void canBeEditedBy(User user) {
        if (!this.createdBy.equals(user) && !Role.ADMIN.equals(user.getRole())) {
            throw new SecurityException("User does not have permission to edit this entry");
        }
    }

    // === SINGLE FACTORY METHOD ===
    public static FinancialEntry create(
            EntryNumber entryNumber,
            RecordType entryType,
            FinancialCategory category,
            Money amount,
            LocalDate entryDate,
            PaymentMethod paymentMethod,
            String description,
            User creator,
            Long whoId,
            Long mainCategoryId,
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
        entry.entryType = entryType;
        entry.category = category;
        entry.originalAmount = amount;
        entry.baseAmount = amount;
        entry.entryDate = entryDate;
        entry.paymentMethod = paymentMethod;
        entry.description = description;
        entry.createdBy = creator;
        entry.createdAt = LocalDateTime.now();
        entry.updatedAt = LocalDateTime.now();

        // Contextual data
        entry.whoId = whoId;
        entry.mainCategoryId = mainCategoryId;
        entry.recipient = recipient;
        entry.country = country;
        entry.city = city;
        entry.specificLocation = specificLocation;
        entry.vendor = vendor;

        entry.validate();
        return entry;
    }

    // === BUSINESS METHODS ===
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
        this.baseAmount = amount;
        this.entryDate = Objects.requireNonNull(entryDate, "Entry date cannot be null");
        this.paymentMethod = Objects.requireNonNull(paymentMethod, "Payment method cannot be null");
        this.description = description;

        updateAudit(updater);
        validate();
    }

    public void updateContext(
            Long whoId,
            Long mainCategoryId,
            String recipient,
            String country,
            String city,
            String specificLocation,
            String vendor,
            User updater
    ) {
        requireEditPermission(updater);

        this.whoId = whoId;
        this.mainCategoryId = mainCategoryId;
        this.recipient = recipient;
        this.country = country;
        this.city = city;
        this.specificLocation = specificLocation;
        this.vendor = vendor;

        updateAudit(updater);
    }

    public void updateMetadata(
            String frequency,
            String priority,
            String tags,
            User updater
    ) {
        requireEditPermission(updater);

        this.frequency = frequency;
        this.priority = priority;
        this.tags = tags;

        updateAudit(updater);
    }

    public void updateReceiptNumber(String receiptNumber, User updater) {
        requireEditPermission(updater);
        this.receiptNumber = receiptNumber;
        updateAudit(updater);
    }

    public void updateExchangeRate(BigDecimal rate, LocalDate rateDate, User updater) {
        requireEditPermission(updater);

        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive");
        }

        this.exchangeRate = rate;
        this.exchangeRateDate = rateDate;

        // Recalculate base amount
        if (originalAmount != null) {
            BigDecimal baseAmountValue = originalAmount.getAmount().multiply(rate);
            this.baseAmount = Money.of(baseAmountValue, BASE_CURRENCY);
        }

        updateAudit(updater);
    }

    // === ATTACHMENT MANAGEMENT ===
    public void addAttachment(FinancialEntryAttachment attachment, User updater) {
        requireEditPermission(updater);
        Objects.requireNonNull(attachment, "Attachment cannot be null");

        if (attachments == null) {
            attachments = new ArrayList<>();
        }
        attachments.add(attachment);
        attachment.associateWithEntry(this);

        updateAudit(updater);
    }

    public void removeAttachment(FinancialEntryAttachment attachment, User updater) {
        requireEditPermission(updater);

        if (attachments != null && attachments.remove(attachment)) {
            attachment.dissociateFromEntry();
            updateAudit(updater);
        }
    }

    // === DOMAIN CHECKS ===
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
        return whoId != null;
    }

    public boolean hasMainCategory() {
        return mainCategoryId != null;
    }

    public boolean isDetailedEntry() {
        return hasWho() && hasMainCategory();
    }

    // === VALIDATION ===
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
        if (createdBy == null) errors.add("Creator is required");
        if (entryDate == null) errors.add("Entry date is required");
        if (entryDate != null && entryDate.isAfter(LocalDate.now())) {
            errors.add("Entry date cannot be in the future");
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException(String.join("; ", errors));
        }
    }

    // === HELPER METHODS ===
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

    // === PRIVATE METHODS ===
    private void requireEditPermission(User user) {
        if (!this.createdBy.equals(user) && !Role.ADMIN.equals(user.getRole())) {
            throw new SecurityException("User does not have permission to edit this entry");
        }
    }

    private void updateAudit(User updater) {
        this.updatedBy = updater;
        this.updatedAt = LocalDateTime.now();
    }

    // === GETTERS ===
    public UUID getId() { return id; }
    public EntryNumber getEntryNumber() { return entryNumber; }
    public RecordType getEntryType() { return entryType; }
    public FinancialCategory getCategory() { return category; }
    public Money getOriginalAmount() { return originalAmount; }
    public Money getBaseAmount() { return baseAmount; }
    public LocalDate getEntryDate() { return entryDate; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getDescription() { return description; }
    public Long getWhoId() { return whoId; }
    public Long getMainCategoryId() { return mainCategoryId; }
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
    public User getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public User getUpdatedBy() { return updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    // === EQUALS/HASHCODE ===
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FinancialEntry)) return false;
        FinancialEntry that = (FinancialEntry) o;
        return Objects.equals(entryNumber, that.entryNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entryNumber);
    }

    @Override
    public String toString() {
        return String.format(
                "FinancialEntry{id=%s, number='%s', type=%s, amount=%s, category=%s, whoId=%s, mainCategoryId=%s}",
                id, entryNumber, entryType, originalAmount,
                category != null ? category.getName() : "null",
                whoId, mainCategoryId
        );
    }
}