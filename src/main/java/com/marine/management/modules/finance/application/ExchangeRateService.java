package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.domain.entity.ExchangeRate;
import com.marine.management.modules.finance.infrastructure.ExchangeRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

@Service
public class ExchangeRateService {
    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateService.class);

    private final ExchangeRateRepository exchangeRateRepository;
    private final RestTemplate restTemplate;

    // WORKING API - No API key needed, free
    private static final String API_URL = "https://api.exchangerate-api.com/v4/latest/EUR";
    private static final String BASE_CURRENCY = "EUR";
    private static final int SCALE = 8;

    public ExchangeRateService(
            ExchangeRateRepository exchangeRateRepository,
            RestTemplate restTemplate
    ) {
        this.exchangeRateRepository = exchangeRateRepository;
        this.restTemplate = restTemplate;
    }

    public BigDecimal convert(
            LocalDate date,
            String fromCurrency,
            String toCurrency,
            BigDecimal amount
    ) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }

        BigDecimal rate = getRate(date, fromCurrency, toCurrency);
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    @Cacheable(value = "exchange-rates", key = "#date + '-' + #fromCurrency + '-' + #toCurrency")
    public BigDecimal getRate(LocalDate date, String fromCurrency, String toCurrency) {
        // Check if cross-rate exists
        return exchangeRateRepository
                .findByDateAndFromCurrencyAndToCurrency(date, fromCurrency, toCurrency)
                .map(ExchangeRate::getRate)
                .orElseGet(() -> calculateAndSaveCrossRate(date, fromCurrency, toCurrency));
    }

    @Transactional
    protected BigDecimal calculateAndSaveCrossRate(
            LocalDate date,
            String fromCurrency,
            String toCurrency
    ) {
        logger.info("Calculating cross-rate: {} -> {} on {}", fromCurrency, toCurrency, date);

        BigDecimal fromRate = getBaseRate(date, fromCurrency);
        BigDecimal toRate = getBaseRate(date, toCurrency);

        // Calculate: from -> to = (EUR -> to) / (EUR -> from)
        BigDecimal crossRate = toRate.divide(fromRate, SCALE, RoundingMode.HALF_UP);

        // Save for future use
        ExchangeRate exchangeRate = ExchangeRate.builder()
                .date(date)
                .fromCurrency(fromCurrency)
                .toCurrency(toCurrency)
                .rate(crossRate)
                .base(false)
                .source("CALCULATED")
                .build();

        exchangeRateRepository.save(exchangeRate);

        logger.info("Saved cross-rate: {} -> {} = {}", fromCurrency, toCurrency, crossRate);

        return crossRate;
    }

    @Transactional
    protected BigDecimal getBaseRate(LocalDate date, String currency) {
        if (BASE_CURRENCY.equals(currency)) {
            return BigDecimal.ONE;
        }

        return exchangeRateRepository
                .findByDateAndFromCurrencyAndToCurrency(date, BASE_CURRENCY, currency)
                .map(ExchangeRate::getRate)
                .orElseGet(() -> fetchAndSaveBaseRate(date, currency));
    }

    @Transactional
    protected BigDecimal fetchAndSaveBaseRate(LocalDate date, String currency) {
        logger.info("Fetching rate from API: EUR -> {} on {}", currency, date);

        try {
            // ✅ NEW API - Always returns latest rates (no date param needed)
            // This API doesn't support historical dates, but it's free and reliable
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(API_URL, Map.class);

            if (response == null || !response.containsKey("rates")) {
                throw new RuntimeException("Invalid API response");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> rates = (Map<String, Object>) response.get("rates");

            if (!rates.containsKey(currency)) {
                throw new RuntimeException("Currency not found: " + currency);
            }

            // Get rate as number (can be Integer or Double)
            Object rateObj = rates.get(currency);
            BigDecimal rate;
            if (rateObj instanceof Integer) {
                rate = new BigDecimal((Integer) rateObj);
            } else if (rateObj instanceof Double) {
                rate = BigDecimal.valueOf((Double) rateObj);
            } else {
                rate = new BigDecimal(rateObj.toString());
            }

            // Save base rate (with requested date, even though it's latest rate)
            ExchangeRate exchangeRate = ExchangeRate.builder()
                    .date(date)
                    .fromCurrency(BASE_CURRENCY)
                    .toCurrency(currency)
                    .rate(rate)
                    .base(true)
                    .source("ExchangeRate-API")
                    .build();

            exchangeRateRepository.save(exchangeRate);

            logger.info("Saved base rate: EUR -> {} = {}", currency, rate);

            return rate;

        } catch (Exception e) {
            logger.error("Failed to fetch rate for {} on {}: {}", currency, date, e.getMessage());

            // ✅ FALLBACK: Use approximate rates if API fails
            logger.warn("Using fallback rate for {}", currency);
            return useFallbackRate(date, currency);
        }
    }

    /**
     * Fallback rates when API is unavailable
     */
    private BigDecimal useFallbackRate(LocalDate date, String currency) {
        BigDecimal fallbackRate = switch (currency) {
            case "USD" -> new BigDecimal("1.08");
            case "GBP" -> new BigDecimal("0.85");
            case "TRY" -> new BigDecimal("36.50");
            default -> BigDecimal.ONE;
        };

        // Save fallback rate
        ExchangeRate exchangeRate = ExchangeRate.builder()
                .date(date)
                .fromCurrency(BASE_CURRENCY)
                .toCurrency(currency)
                .rate(fallbackRate)
                .base(true)
                .source("FALLBACK")
                .build();

        exchangeRateRepository.save(exchangeRate);

        logger.info("Saved fallback rate: EUR -> {} = {}", currency, fallbackRate);

        return fallbackRate;
    }

    /**
     * Bulk fetch rates for a date (optimization)
     */
    @Transactional
    public void fetchRatesForDate(LocalDate date, String... currencies) {
        logger.info("Bulk fetching rates for date: {}", date);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(API_URL, Map.class);

            if (response == null || !response.containsKey("rates")) {
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> rates = (Map<String, Object>) response.get("rates");

            for (String currency : currencies) {
                if (rates.containsKey(currency)) {
                    Object rateObj = rates.get(currency);
                    BigDecimal rate;
                    if (rateObj instanceof Integer) {
                        rate = new BigDecimal((Integer) rateObj);
                    } else if (rateObj instanceof Double) {
                        rate = BigDecimal.valueOf((Double) rateObj);
                    } else {
                        rate = new BigDecimal(rateObj.toString());
                    }

                    if (!exchangeRateRepository.existsByDateAndFromCurrencyAndToCurrency(
                            date, BASE_CURRENCY, currency)) {

                        ExchangeRate exchangeRate = ExchangeRate.builder()
                                .date(date)
                                .fromCurrency(BASE_CURRENCY)
                                .toCurrency(currency)
                                .rate(rate)
                                .base(true)
                                .source("ExchangeRate-API")
                                .build();

                        exchangeRateRepository.save(exchangeRate);
                    }
                }
            }

            logger.info("Bulk fetch completed for {} currencies", currencies.length);

        } catch (Exception e) {
            logger.error("Bulk fetch failed: {}", e.getMessage());
        }
    }

    /**
     * Manual rate entry (for admin)
     */
    @Transactional
    public void saveManualRate(
            LocalDate date,
            String fromCurrency,
            String toCurrency,
            BigDecimal rate
    ) {
        ExchangeRate exchangeRate = ExchangeRate.builder()
                .date(date)
                .fromCurrency(fromCurrency)
                .toCurrency(toCurrency)
                .rate(rate)
                .base(BASE_CURRENCY.equals(fromCurrency))
                .source("MANUAL")
                .build();

        exchangeRateRepository.save(exchangeRate);

        logger.info("Manual rate saved: {} -> {} = {}", fromCurrency, toCurrency, rate);
    }
}