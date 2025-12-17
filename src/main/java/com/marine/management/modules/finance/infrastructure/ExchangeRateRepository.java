package com.marine.management.modules.finance.infrastructure;

import com.marine.management.modules.finance.domain.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findByDateAndFromCurrencyAndToCurrency(
            LocalDate date,
            String fromCurrency,
            String toCurrency
    );

    boolean existsByDateAndFromCurrencyAndToCurrency(
            LocalDate date,
            String fromCurrency,
            String toCurrency
    );

    List<ExchangeRate> findByDate(LocalDate date);

    @Query("SELECT e FROM ExchangeRate e WHERE e.date = :date AND e.fromCurrency = 'EUR' AND e.base = true")
    List<ExchangeRate> findBaseRatesByDate(LocalDate date);

    void deleteByDateBefore(LocalDate date);
}