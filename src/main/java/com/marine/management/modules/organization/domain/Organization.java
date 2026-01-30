package com.marine.management.modules.organization.domain;

import com.marine.management.shared.domain.BaseAuditedEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Organization entity - tenant root.
 *
 * DESIGN DECISIONS:
 * - Unique yacht_name: Assumes global namespace (one yacht name across all orgs)
 * - Immutable critical fields: yachtName, flagCountry, baseCurrency (use rename/change methods)
 * - Subscription guard: isActive() checks expiration
 */
@Entity
@Table(
        name = "organizations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_organizations_yacht_name", columnNames = "yacht_name")
        },
        indexes = {
                @Index(name = "idx_organizations_yacht_name", columnList = "yacht_name")
        }
)
public class Organization extends BaseAuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "yacht_name", nullable = false, length = 100)
    private String yachtName;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "flag_country", nullable = false, length = 2)
    private String flagCountry;

    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    @Column(name = "yacht_type", length = 50)
    private String yachtType;

    @Column(name = "yacht_length")
    private Integer yachtLength;

    @Column(name = "home_marina", length = 200)
    private String homeMarina;

    @Column(name = "current_location", length = 200)
    private String currentLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", nullable = false, length = 30)
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.TRIAL;

    @Column(name = "subscription_expires_at")
    private LocalDate subscriptionExpiresAt;

    @Column(nullable = false)
    private Boolean active = true;

    protected Organization() {}

    private Organization(
            String yachtName,
            String companyName,
            String flagCountry,
            String baseCurrency
    ) {
        this.yachtName = validateYachtName(yachtName);
        this.companyName = companyName;
        this.flagCountry = validateCountryCode(flagCountry);
        this.baseCurrency = validateCurrencyCode(baseCurrency);
        this.subscriptionStatus = SubscriptionStatus.TRIAL;
        this.subscriptionExpiresAt = LocalDate.now().plusDays(30);
        this.active = true;
    }

    @Override
    public Object getId() {
        return id;
    }

    public Long getOrganizationId() {
        return id;
    }

    public static Organization create(
            String yachtName,
            String companyName,
            String flagCountry,
            String baseCurrency
    ) {
        return new Organization(yachtName, companyName, flagCountry, baseCurrency);
    }

    public static Organization create(String yachtName, String flagCountry, String baseCurrency) {
        return new Organization(yachtName, null, flagCountry, baseCurrency);
    }

    private String validateYachtName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Yacht name cannot be empty");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("Yacht name cannot exceed 100 characters");
        }
        return name.trim();
    }

    private String validateCountryCode(String code) {
        if (code == null || code.length() != 2) {
            throw new IllegalArgumentException("Country code must be 2-character ISO 3166-1 alpha-2");
        }
        return code.toUpperCase();
    }

    private String validateCurrencyCode(String code) {
        if (code == null || code.length() != 3) {
            throw new IllegalArgumentException("Currency code must be 3-character ISO 4217");
        }
        return code.toUpperCase();
    }

    public void renameYacht(String newName) {
        this.yachtName = validateYachtName(newName);
    }

    public void changeFlagCountry(String newCountry) {
        this.flagCountry = validateCountryCode(newCountry);
    }

    public void changeBaseCurrency(String newCurrency) {
        this.baseCurrency = validateCurrencyCode(newCurrency);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isActive(LocalDate today) {
        if (!active) return false;
        if (subscriptionStatus == SubscriptionStatus.SUSPENDED) return false;
        if (subscriptionExpiresAt != null && subscriptionExpiresAt.isBefore(today)) return false;
        return true;
    }

    public void updateDetails(
            String companyName,
            String yachtType,
            Integer yachtLength,
            String homeMarina,
            String currentLocation
    ) {
        this.companyName = companyName;
        this.yachtType = yachtType;
        this.yachtLength = yachtLength;
        this.homeMarina = homeMarina;
        this.currentLocation = currentLocation;
    }

    public void upgradeSubscription(SubscriptionStatus newStatus, LocalDate expiresAt) {
        Objects.requireNonNull(newStatus);
        if (newStatus == SubscriptionStatus.ACTIVE && expiresAt == null) {
            throw new IllegalArgumentException("Active subscription requires expiration date");
        }
        this.subscriptionStatus = newStatus;
        this.subscriptionExpiresAt = expiresAt;
    }

    public void suspendSubscription() {
        this.subscriptionStatus = SubscriptionStatus.SUSPENDED;
    }

    public String getYachtName() { return yachtName; }
    public String getCompanyName() { return companyName; }
    public String getFlagCountry() { return flagCountry; }
    public String getBaseCurrency() { return baseCurrency; }
    public String getYachtType() { return yachtType; }
    public Integer getYachtLength() { return yachtLength; }
    public String getHomeMarina() { return homeMarina; }
    public String getCurrentLocation() { return currentLocation; }
    public SubscriptionStatus getSubscriptionStatus() { return subscriptionStatus; }
    public LocalDate getSubscriptionExpiresAt() { return subscriptionExpiresAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Organization other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return String.format("Organization{id=%d, yachtName='%s', status=%s}",
                id, yachtName, subscriptionStatus);
    }
}