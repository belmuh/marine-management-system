package com.marine.management.modules.organization.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Organization entity representing a yacht/company (tenant).
 *
 * CRITICAL DESIGN:
 * - This is the ROOT entity (NOT tenant-isolated itself)
 * - Does NOT extend BaseTenantEntity
 * - Does NOT have tenant_id column
 * - All other entities reference this via tenant_id
 *
 * Database Constraints:
 * - yacht_name: UNIQUE, NOT NULL
 * - flag_country: NOT NULL (ISO 3166-1 alpha-2)
 * - base_currency: NOT NULL (ISO 4217)
 */
@Entity
@Table(
        name = "organizations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_organizations_yacht_name",
                        columnNames = "yacht_name"
                )
        },
        indexes = {
                @Index(name = "idx_organizations_yacht_name", columnList = "yacht_name"),
                @Index(name = "idx_organizations_active", columnList = "active")
        }
)
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "yacht_name", nullable = false, length = 100)
    private String yachtName;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "flag_country", nullable = false, length = 2)
    private String flagCountry;  // ISO 3166-1 alpha-2 (TR, US, GB)

    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;  // ISO 4217 (EUR, USD, TRY)

    @Column(name = "yacht_type", length = 50)
    private String yachtType;

    @Column(name = "yacht_length")
    private Integer yachtLength;

    @Column(name = "home_marina", length = 200)
    private String homeMarina;

    @Column(name = "current_location", length = 200)
    private String currentLocation;

    @Column(name = "subscription_status", nullable = false, length = 20)
    private String subscriptionStatus = "FREE";

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Organization() {}

    // Private constructor for factory method
    private Organization(
            String yachtName,
            String companyName,
            String flagCountry,
            String baseCurrency,
            String subscriptionStatus
    ) {
        this.yachtName = Objects.requireNonNull(yachtName, "Yacht name cannot be null");
        this.companyName = companyName;
        this.flagCountry = Objects.requireNonNull(flagCountry, "Flag country cannot be null");
        this.baseCurrency = Objects.requireNonNull(baseCurrency, "Base currency cannot be null");
        this.subscriptionStatus = subscriptionStatus != null ? subscriptionStatus : "FREE";
        this.active = true;
    }

    // ✅ NEW: Factory method for creating organizations
    /**
     * Creates a new organization.
     *
     * @param yachtName yacht/organization name (unique)
     * @param companyName company name (optional)
     * @param flagCountry ISO 3166-1 alpha-2 country code (e.g., "TR", "US")
     * @param baseCurrency ISO 4217 currency code (e.g., "EUR", "USD")
     * @param subscriptionStatus subscription tier (e.g., "FREE", "PREMIUM")
     * @return new Organization instance
     */
    public static Organization create(
            String yachtName,
            String companyName,
            String flagCountry,
            String baseCurrency,
            String subscriptionStatus
    ) {
        Organization org = new Organization(
                yachtName,
                companyName,
                flagCountry,
                baseCurrency,
                subscriptionStatus
        );

        org.validate();
        return org;
    }

    // ✅ NEW: Simplified factory for basic organization
    public static Organization create(
            String yachtName,
            String flagCountry,
            String baseCurrency
    ) {
        return create(yachtName, null, flagCountry, baseCurrency, "FREE");
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ✅ NEW: Validation method
    private void validate() {
        if (yachtName == null || yachtName.trim().isEmpty()) {
            throw new IllegalArgumentException("Yacht name cannot be empty");
        }
        if (yachtName.length() > 100) {
            throw new IllegalArgumentException("Yacht name cannot exceed 100 characters");
        }
        if (flagCountry == null || flagCountry.length() != 2) {
            throw new IllegalArgumentException("Flag country must be 2-character ISO code");
        }
        if (baseCurrency == null || baseCurrency.length() != 3) {
            throw new IllegalArgumentException("Base currency must be 3-character ISO code");
        }
    }

    // ✅ NEW: Business methods
    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isActive() {
        return active != null && active;
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

    public void upgradeSubscription(String newStatus) {
        this.subscriptionStatus = Objects.requireNonNull(newStatus, "Subscription status cannot be null");
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getYachtName() { return yachtName; }
    public void setYachtName(String yachtName) { this.yachtName = yachtName; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getFlagCountry() { return flagCountry; }
    public void setFlagCountry(String flagCountry) { this.flagCountry = flagCountry; }
    public String getBaseCurrency() { return baseCurrency; }
    public void setBaseCurrency(String baseCurrency) { this.baseCurrency = baseCurrency; }
    public String getYachtType() { return yachtType; }
    public void setYachtType(String yachtType) { this.yachtType = yachtType; }
    public Integer getYachtLength() { return yachtLength; }
    public void setYachtLength(Integer yachtLength) { this.yachtLength = yachtLength; }
    public String getHomeMarina() { return homeMarina; }
    public void setHomeMarina(String homeMarina) { this.homeMarina = homeMarina; }
    public String getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }
    public String getSubscriptionStatus() { return subscriptionStatus; }
    public void setSubscriptionStatus(String subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Organization org)) return false;
        return Objects.equals(id, org.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Organization{id=%d, yachtName='%s'}", id, yachtName);
    }
}