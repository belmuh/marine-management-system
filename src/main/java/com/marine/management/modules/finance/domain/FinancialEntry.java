package com.marine.management.modules.finance.domain;

import com.marine.management.modules.finance.domain.model.Money;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.kernel.security.Role;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name="financial_entries")
public class FinancialEntry {

    public static final String BASE_CURRENCY = "EUR";
    // IMMUTABLE FIELDS

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "entry_number"))
    private EntryNumber entryNumber; //"FE-2024-001"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryType entryType; // income-expense

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // MUTABLE FIELDS

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private FinancialCategory category;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "original_amount")),
            @AttributeOverride(name = "currencyCode", column = @Column(name = "original_currency"))
    })
    private Money originalAmount;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "base_amount")),
            @AttributeOverride(name = "currencyCode", column =  @Column(name = "base_currency"))
    })
    private Money baseAmount;

    @Column(name = "exchange_rate", precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "exchange_rate_date")
    private LocalDate exchangeRateDate;

    @Column(name = "receipt_number", length = 100)
    private String receiptNumber;  //Fis/Fatura No

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FinancialEntryAttachment> attachments = new ArrayList<>();

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private LocalDate entryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    // JPA için
    protected FinancialEntry() {}

    // FACTORY METHODS
    public static FinancialEntry create(
            EntryNumber entryNumber,
            EntryType entryType,
            FinancialCategory category,
            Money amount,
            LocalDate entryDate,
            User creator,
            String description
    ) {
        FinancialEntry entry = new FinancialEntry();
        entry.entryNumber = entryNumber;
        entry.entryType = entryType;
        entry.category = category;
        entry.originalAmount = amount;
        entry.baseAmount = amount; // Başlangıçta aynı
        entry.entryDate = entryDate;
        entry.createdBy = creator;
        entry.description = description;
        entry.createdAt = LocalDateTime.now();
        entry.updatedAt = LocalDateTime.now();
        entry.validate();
        return entry;
    }

    // BUSINESS METHODS

    public void updateDetails(
            FinancialCategory category,
            Money amount,
            LocalDate entryDate,
            String description,
            User user
    ) {
        canBeEditedBy(user);

        this.category = category;
        this.originalAmount = amount;
        this.baseAmount = amount;
        this.entryDate = entryDate;
        this.description = description;
        this.updatedBy = user;
        this.updatedAt = LocalDateTime.now();

        validate();
    }

    public void updateReceiptNumber(String receiptNumber, User user) {
        canBeEditedBy(user);
        this.receiptNumber = receiptNumber;
        this.updatedBy = user;
        this.updatedAt = LocalDateTime.now();
    }

    public void setExchangeRate(BigDecimal rate, LocalDate rateDate, User user) {
        canBeEditedBy(user);
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive");
        }
        this.exchangeRate = rate;
        this.exchangeRateDate = rateDate;

        // Base amount'u yeniden hesapla
        if (originalAmount != null) {
            BigDecimal baseAmountValue = originalAmount.amount().multiply(rate);
            this.baseAmount = Money.of(
                    baseAmountValue.toString(),
                    BASE_CURRENCY // Base currency
            );
        }

        this.updatedBy = user;
        this.updatedAt = LocalDateTime.now();
    }

    public void addAttachment(FinancialEntryAttachment attachment, User user) {
        canBeEditedBy(user);
        Objects.requireNonNull(attachment, "Attachment cannot be null");
        if (attachments == null) {
            attachments = new ArrayList<>();
        }
        attachments.add(attachment);
        attachment.setEntry(this);
        this.updatedBy = user;
        this.updatedAt = LocalDateTime.now();
    }

    public void removeAttachment(FinancialEntryAttachment attachment, User user) {
        canBeEditedBy(user);
        if (attachments != null) {
            attachments.remove(attachment);
            attachment.setEntry(null);
            this.updatedBy = user;
            this.updatedAt = LocalDateTime.now();
        }
    }


    public boolean isIncome(){
        return this.entryType == EntryType.INCOME;
    }

    public boolean isExpense() {
        return this.entryType == EntryType.EXPENSE;
    }

    public void canBeEditedBy(User user) {
        if (!this.createdBy.equals(user) && !user.getRole().equals(Role.ADMIN)) {
            throw new SecurityException("User does not have permission to edit this entry");
        }

    }

    public boolean hasAttachments() {
        return attachments != null && !attachments.isEmpty();
    }

    // GETTERS

    public UUID getId() {
        return id;
    }

    public EntryNumber getEntryNumber() {
        return entryNumber;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public FinancialCategory getCategory() {
        return category;
    }

    public Money getOriginalAmount() {
        return originalAmount;
    }

    public Money getBaseAmount() {
        return baseAmount;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public LocalDate getExchangeRateDate() {
        return exchangeRateDate;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public List<FinancialEntryAttachment> getAttachments() {
        return attachments != null ? List.copyOf(attachments) : List.of();
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public User getUpdatedBy() {
        return updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    // VALIDATE

    public void validate() {
        if (entryNumber == null ) {
            throw new IllegalStateException("Entry number is required");
        }
        if (entryType == null) {
            throw new IllegalStateException("Entry type is required");
        }
        if (originalAmount == null) {
            throw new IllegalStateException("Amount is required");
        }
        if (originalAmount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Amount must be positive");
        }
        if (category == null) {
            throw new IllegalStateException("Category is required");
        }
        if (!category.isActive()) {
            throw new IllegalStateException("Category must be active");
        }
        if (createdBy == null) {
            throw new IllegalStateException("Creator user is required");
        }
        if (entryDate == null) {
            throw new IllegalStateException("Entry date is required");
        }
        if (entryDate.isAfter(LocalDate.now())) {
            throw new IllegalStateException("Entry date cannot be in the future");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FinancialEntry)) return false;
        FinancialEntry that = (FinancialEntry) o;
        return entryNumber != null && entryNumber.equals(that.entryNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entryNumber);
    }

    @Override
    public String toString() {
        return String.format(
                "FinancialEntry{id=%s, number='%s', type=%s, amount=%s}",
                id, entryNumber, entryType, originalAmount
        );
    }

}
