package com.marine.management.modules.finance.domain.entity;

import jakarta.persistence.*;
        import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "exchange_rates",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"date", "from_currency", "to_currency"}
        ),
        indexes = {
                @Index(name = "idx_rate_lookup", columnList = "date, from_currency, to_currency")
        }
)
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "from_currency", nullable = false, length = 3)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 3)
    private String toCurrency;

    @Column(nullable = false, precision = 12, scale = 8)
    private BigDecimal rate;

    @Column(name = "base")
    private Boolean base = false;

    @Column(length = 50)
    private String source; // 'ECB', 'MANUAL', 'CALCULATED'

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public ExchangeRate() {
    }

    public ExchangeRate(LocalDate date, String fromCurrency, String toCurrency, BigDecimal rate, Boolean base, String source) {
        this.date = date;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.rate = rate;
        this.base = base;
        this.source = source;
    }


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getFromCurrency() {
        return fromCurrency;
    }

    public void setFromCurrency(String fromCurrency) {
        this.fromCurrency = fromCurrency;
    }

    public String getToCurrency() {
        return toCurrency;
    }

    public void setToCurrency(String toCurrency) {
        this.toCurrency = toCurrency;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public Boolean getBase() {
        return base;
    }

    public void setBase(Boolean base) {
        this.base = base;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LocalDate date;
        private String fromCurrency;
        private String toCurrency;
        private BigDecimal rate;
        private Boolean base = false;
        private String source;

        public Builder date(LocalDate date) {
            this.date = date;
            return this;
        }

        public Builder fromCurrency(String fromCurrency) {
            this.fromCurrency = fromCurrency;
            return this;
        }

        public Builder toCurrency(String toCurrency) {
            this.toCurrency = toCurrency;
            return this;
        }

        public Builder rate(BigDecimal rate) {
            this.rate = rate;
            return this;
        }

        public Builder base(Boolean base) {
            this.base = base;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public ExchangeRate build() {
            ExchangeRate exchangeRate = new ExchangeRate();
            exchangeRate.setDate(this.date);
            exchangeRate.setFromCurrency(this.fromCurrency);
            exchangeRate.setToCurrency(this.toCurrency);
            exchangeRate.setRate(this.rate);
            exchangeRate.setBase(this.base);
            exchangeRate.setSource(this.source);
            return exchangeRate;
        }
    }
}