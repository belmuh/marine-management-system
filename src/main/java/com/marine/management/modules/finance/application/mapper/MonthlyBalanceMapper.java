package com.marine.management.modules.finance.application.mapper;

import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.vo.Money;
import com.marine.management.modules.finance.domain.vo.MonthlyBalance;
import com.marine.management.modules.finance.infrastructure.FinancialEntryReportRepository.MonthlyTotalProjection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Maps infrastructure projections to domain MonthlyBalance value objects.
 *
 * Responsibility: Transform database query results into rich domain objects.
 */
@Component
public class MonthlyBalanceMapper {

    private static final Logger log = LoggerFactory.getLogger(MonthlyBalanceMapper.class);
    private static final String DEFAULT_CURRENCY = "EUR";

    /**
     * Maps infrastructure projections to domain objects.
     *
     * Groups projections by month, separates income/expense by RecordType,
     * and creates immutable MonthlyBalance objects.
     *
     * @param projections List of monthly total projections from database
     * @return Sorted list of MonthlyBalance objects (chronological order)
     */
    public List<MonthlyBalance> toDomain(List<MonthlyTotalProjection> projections) {
        if (projections == null || projections.isEmpty()) {
            log.debug("Empty projections provided, returning empty list");
            return Collections.emptyList();
        }

        // Group by YearMonth
        Map<YearMonth, MutableMonthlyData> monthMap = new TreeMap<>(); // TreeMap for automatic sorting

        for (MonthlyTotalProjection projection : projections) {
            YearMonth yearMonth = YearMonth.of(projection.getYear(), projection.getMonth());

            monthMap.computeIfAbsent(yearMonth, MutableMonthlyData::new);

            BigDecimal amount = projection.getTotal() != null ? projection.getTotal() : BigDecimal.ZERO;

            if (projection.getEntryType() == RecordType.INCOME) {
                monthMap.get(yearMonth).income = Money.of(amount, DEFAULT_CURRENCY);
            } else if (projection.getEntryType() == RecordType.EXPENSE) {
                monthMap.get(yearMonth).expense = Money.of(amount, DEFAULT_CURRENCY);
            }
        }

        // Convert to immutable domain objects
        List<MonthlyBalance> result = monthMap.values().stream()
                .map(data -> MonthlyBalance.of(
                        data.month,
                        data.income != null ? data.income : Money.zero(DEFAULT_CURRENCY),
                        data.expense != null ? data.expense : Money.zero(DEFAULT_CURRENCY)
                ))
                .collect(Collectors.toList());

        log.debug("Mapped {} projections to {} monthly balances", projections.size(), result.size());

        return result;
    }

    /**
     * Maps with custom currency.
     *
     * @param projections List of monthly total projections
     * @param currency Currency code (e.g., "EUR", "USD")
     * @return Sorted list of MonthlyBalance objects
     */
    public List<MonthlyBalance> toDomain(List<MonthlyTotalProjection> projections, String currency) {
        if (projections == null || projections.isEmpty()) {
            return Collections.emptyList();
        }

        String currencyToUse = currency != null ? currency : DEFAULT_CURRENCY;

        Map<YearMonth, MutableMonthlyData> monthMap = new TreeMap<>();

        for (MonthlyTotalProjection projection : projections) {
            YearMonth yearMonth = YearMonth.of(projection.getYear(), projection.getMonth());

            monthMap.computeIfAbsent(yearMonth, MutableMonthlyData::new);

            BigDecimal amount = projection.getTotal() != null ? projection.getTotal() : BigDecimal.ZERO;

            if (projection.getEntryType() == RecordType.INCOME) {
                monthMap.get(yearMonth).income = Money.of(amount, currencyToUse);
            } else if (projection.getEntryType() == RecordType.EXPENSE) {
                monthMap.get(yearMonth).expense = Money.of(amount, currencyToUse);
            }
        }

        List<MonthlyBalance> result = monthMap.values().stream()
                .map(data -> MonthlyBalance.of(
                        data.month,
                        data.income != null ? data.income : Money.zero(currencyToUse),
                        data.expense != null ? data.expense : Money.zero(currencyToUse)
                ))
                .collect(Collectors.toList());

        log.debug("Mapped {} projections to {} monthly balances with currency {}",
                projections.size(), result.size(), currencyToUse);

        return result;
    }

    /**
     * Fills missing months with zero balances.
     *
     * Useful for creating complete time series even when some months have no data.
     *
     * @param monthlyBalances Existing monthly balances (may have gaps)
     * @param startMonth Start of the period
     * @param endMonth End of the period
     * @param currency Currency to use for zero balances
     * @return Complete list with all months filled
     */
    public List<MonthlyBalance> fillMissingMonths(
            List<MonthlyBalance> monthlyBalances,
            YearMonth startMonth,
            YearMonth endMonth,
            String currency
    ) {
        if (startMonth.isAfter(endMonth)) {
            throw new IllegalArgumentException("Start month must be before or equal to end month");
        }

        Map<YearMonth, MonthlyBalance> balanceMap = monthlyBalances.stream()
                .collect(Collectors.toMap(MonthlyBalance::getMonth, mb -> mb));

        List<MonthlyBalance> result = new ArrayList<>();
        YearMonth current = startMonth;

        while (!current.isAfter(endMonth)) {
            if (balanceMap.containsKey(current)) {
                result.add(balanceMap.get(current));
            } else {
                result.add(MonthlyBalance.zero(current, currency));
            }
            current = current.plusMonths(1);
        }

        log.debug("Filled missing months: original {} balances, filled to {} balances",
                monthlyBalances.size(), result.size());

        return result;
    }

    /**
     * Mutable helper class for mapping only.
     *
     * Used temporarily during transformation, then converted to immutable MonthlyBalance.
     * Package-private to prevent external usage.
     */
    private static class MutableMonthlyData {
        final YearMonth month;
        Money income;
        Money expense;

        MutableMonthlyData(YearMonth month) {
            this.month = month;
        }
    }
}