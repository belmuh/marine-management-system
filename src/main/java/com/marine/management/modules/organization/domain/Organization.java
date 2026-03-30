package com.marine.management.modules.organization.domain;

import com.marine.management.shared.domain.BaseAuditedEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Organization entity - tenant root.
 *
 * DESIGN DECISIONS:
 * - Unique yacht_name: global namespace (one yacht name across all orgs)
 * - Immutable critical fields set during onboarding: flagCountry, baseCurrency, timezone, financialYearStartMonth
 *   (changing these = new tenant, because financial history depends on them)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "yacht_type", length = 50)
    private YachtType yachtType;

    @Column(name = "yacht_length")
    private Integer yachtLength;

    @Column(name = "home_marina", length = 200)
    private String homeMarina;

    @Column(name = "current_location", length = 200)
    private String currentLocation;

    /**
     * Timezone for this tenant. Set during onboarding, immutable after creation.
     * All date/time operations for this tenant use this timezone.
     */
    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone = "Europe/Istanbul";

    /**
     * Month when the financial year starts (1-12). Set during onboarding, immutable after creation.
     * Used for annual budget calculations and carry-over balance.
     */
    @Column(name = "financial_year_start_month", nullable = false)
    private Integer financialYearStartMonth = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", nullable = false, length = 30)
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.TRIAL;

    @Column(name = "subscription_expires_at")
    private LocalDate subscriptionExpiresAt;

    @Column(nullable = false)
    private Boolean active = true;

    /**
     * Whether manager approval is enabled for this tenant.
     * If false, Captain approves everything directly.
     */
    @Column(name = "manager_approval_enabled")
    private boolean managerApprovalEnabled = false;

    /**
     * Approval limit amount.
     * If entry amount > limit AND managerApprovalEnabled, requires manager approval.
     * If null, no limit (all go directly to APPROVED after captain).
     */
    @Column(name = "approval_limit", precision = 15, scale = 2)
    private BigDecimal approvalLimit;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted = false;

    protected Organization() {}

    private Organization(
            String yachtName,
            String companyName,
            String flagCountry,
            String baseCurrency,
            String timezone,
            Integer financialYearStartMonth
    ) {
        this.yachtName = validateYachtName(yachtName);
        this.companyName = companyName;
        this.flagCountry = validateCountryCode(flagCountry);
        this.baseCurrency = validateCurrencyCode(baseCurrency);
        this.timezone = validateTimezone(timezone);
        this.financialYearStartMonth = validateFinancialYearStartMonth(financialYearStartMonth);
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

    /**
     * Full factory method for wizard-based registration.
     */
    public static Organization create(
            String yachtName,
            String companyName,
            String flagCountry,
            String baseCurrency,
            String timezone,
            Integer financialYearStartMonth
    ) {
        return new Organization(yachtName, companyName, flagCountry, baseCurrency, timezone, financialYearStartMonth);
    }

    /**
     * Simplified factory method (uses defaults for timezone/financialYear).
     */
    public static Organization create(
            String yachtName,
            String companyName,
            String flagCountry,
            String baseCurrency
    ) {
        return new Organization(yachtName, companyName, flagCountry, baseCurrency, "Europe/Istanbul", 1);
    }

    public static Organization create(String yachtName, String flagCountry, String baseCurrency) {
        return new Organization(yachtName, null, flagCountry, baseCurrency, "Europe/Istanbul", 1);
    }

    /**
     * Minimal factory for registration - only yacht name required.
     * Onboarding will be completed later with all setup details.
     */
    public static Organization createMinimal(String yachtName) {
        return new Organization(yachtName, null, "TR", "EUR", "Europe/Istanbul", 1);
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

    private String validateTimezone(String tz) {
        if (tz == null || tz.trim().isEmpty()) {
            return "Europe/Istanbul";
        }
        if (!java.time.ZoneId.getAvailableZoneIds().contains(tz)) {
            throw new IllegalArgumentException("Invalid timezone: " + tz);
        }
        return tz;
    }

    private Integer validateFinancialYearStartMonth(Integer month) {
        if (month == null) {
            return 1;
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Financial year start month must be between 1 and 12");
        }
        return month;
    }

    public void updateDetails(
            String companyName,
            YachtType yachtType,
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

    public void enableManagerApproval(BigDecimal limit) {
        this.managerApprovalEnabled = true;
        this.approvalLimit = limit;
    }

    public void disableManagerApproval() {
        this.managerApprovalEnabled = false;
        this.approvalLimit = null;
    }

    public void completeOnboarding() {
        this.onboardingCompleted = true;
    }

    public void completeSetup(
            String companyName,
            String flagCountry,
            String baseCurrency,
            YachtType yachtType,
            Integer yachtLength,
            String homeMarina,
            String timezone,
            Integer financialYearStartMonth,
            boolean managerApprovalEnabled,
            BigDecimal approvalLimit
    ) {
        this.companyName = companyName;
        this.flagCountry = validateCountryCode(flagCountry);
        this.baseCurrency = validateCurrencyCode(baseCurrency);
        this.yachtType = yachtType;
        this.yachtLength = yachtLength;
        this.homeMarina = homeMarina;
        this.timezone = validateTimezone(timezone);
        this.financialYearStartMonth = validateFinancialYearStartMonth(financialYearStartMonth);
        if (managerApprovalEnabled) {
            enableManagerApproval(approvalLimit);
        }
        this.onboardingCompleted = true;
    }

    public String getYachtName() { return yachtName; }
    public String getCompanyName() { return companyName; }
    public String getFlagCountry() { return flagCountry; }
    public String getBaseCurrency() { return baseCurrency; }
    public YachtType getYachtType() { return yachtType; }
    public Integer getYachtLength() { return yachtLength; }
    public String getHomeMarina() { return homeMarina; }
    public String getCurrentLocation() { return currentLocation; }
    public String getTimezone() { return timezone; }
    public Integer getFinancialYearStartMonth() { return financialYearStartMonth; }
    public SubscriptionStatus getSubscriptionStatus() { return subscriptionStatus; }
    public LocalDate getSubscriptionExpiresAt() { return subscriptionExpiresAt; }
    public boolean isManagerApprovalEnabled() { return managerApprovalEnabled; }
    public void setManagerApprovalEnabled(boolean managerApprovalEnabled) {
        this.managerApprovalEnabled = managerApprovalEnabled;
    }
    public BigDecimal getApprovalLimit() { return approvalLimit;}
    public void setApprovalLimit(BigDecimal approvalLimit) { this.approvalLimit = approvalLimit; }
    public boolean isOnboardingCompleted() { return onboardingCompleted; }

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