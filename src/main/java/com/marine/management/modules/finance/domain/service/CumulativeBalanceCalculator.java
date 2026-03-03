package com.marine.management.modules.finance.domain.service;

import com.marine.management.modules.finance.domain.vo.Balance;
import com.marine.management.modules.finance.domain.vo.CumulativeBalance;
import com.marine.management.modules.finance.domain.vo.MonthlyBalance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Domain Service: Calculates cumulative balance over time.
 *
 * PURE DOMAIN LOGIC - No infrastructure dependencies!
 *
 * Responsibility: Transform monthly balances into cumulative balances
 * by maintaining a running total.
 */
@Component
public class CumulativeBalanceCalculator {

    private static final Logger log = LoggerFactory.getLogger(CumulativeBalanceCalculator.class);

    /**
     * Calculates cumulative balance from monthly data.
     *
     * Starting from zero, adds each month's net balance to calculate
     * running cumulative total.
     *
     * @param monthlyBalances List of monthly balances (must be chronologically sorted)
     * @return List of cumulative balances
     * @throws IllegalArgumentException if monthlyBalances is null or contains null elements
     */
    public List<CumulativeBalance> calculate(List<MonthlyBalance> monthlyBalances) {
        if (monthlyBalances == null) {
            log.warn("Null monthly balances provided, returning empty list");
            return Collections.emptyList();
        }

        if (monthlyBalances.isEmpty()) {
            log.debug("Empty monthly balances provided, returning empty list");
            return Collections.emptyList();
        }

        validateMonthlyBalances(monthlyBalances);

        String currency = monthlyBalances.get(0).getIncome().getCurrencyCode();
        Balance runningBalance = Balance.zero(currency); // ✅ Balance instead of Money

        List<CumulativeBalance> result = new ArrayList<>(monthlyBalances.size());

        for (MonthlyBalance monthly : monthlyBalances) {
            runningBalance = runningBalance
                    .add(monthly.getIncome())      // ✅ Balance.add(Money)
                    .subtract(monthly.getExpense()); // ✅ Balance.subtract(Money)

            CumulativeBalance cumulative = CumulativeBalance.of(monthly, runningBalance);
            result.add(cumulative);

            if (cumulative.isCritical()) {
                log.warn("CRITICAL: Cumulative balance reached critical level: {} at {}",
                        cumulative.getCumulativeBalance(), cumulative.getMonth());
            }
        }

        log.debug("Calculated cumulative balance for {} months. Final balance: {}",
                result.size(),
                result.isEmpty() ? "N/A" : result.get(result.size() - 1).getCumulativeBalance());

        return result;
    }

    /**
     * Calculates cumulative balance with an initial starting balance.
     *
     * Useful for continuing calculations from a known starting point
     * (e.g., previous year's ending balance).
     *
     * @param initialBalance Starting balance before first month
     * @param monthlyBalances List of monthly balances (must be chronologically sorted)
     * @return List of cumulative balances
     * @throws IllegalArgumentException if parameters are invalid
     */
    public List<CumulativeBalance> calculateWithInitial(
            Balance initialBalance, // ✅ Balance instead of Money
            List<MonthlyBalance> monthlyBalances
    ) {
        if (initialBalance == null) {
            throw new IllegalArgumentException("Initial balance cannot be null");
        }

        if (monthlyBalances == null || monthlyBalances.isEmpty()) {
            log.debug("No monthly balances to calculate, returning empty list");
            return Collections.emptyList();
        }

        validateMonthlyBalances(monthlyBalances);

        Balance runningBalance = initialBalance; // ✅ Balance
        List<CumulativeBalance> result = new ArrayList<>(monthlyBalances.size());

        log.debug("Starting cumulative calculation with initial balance: {}", initialBalance);

        for (MonthlyBalance monthly : monthlyBalances) {
            runningBalance = runningBalance
                    .add(monthly.getIncome())
                    .subtract(monthly.getExpense());

            CumulativeBalance cumulative = CumulativeBalance.of(monthly, runningBalance);
            result.add(cumulative);

            if (cumulative.isCritical()) {
                log.warn("CRITICAL: Cumulative balance reached critical level: {} at {}",
                        cumulative.getCumulativeBalance(), cumulative.getMonth());
            }
        }

        log.debug("Calculated cumulative balance for {} months. Final balance: {}",
                result.size(), result.get(result.size() - 1).getCumulativeBalance());

        return result;
    }

    /**
     * Calculates the break-even point (month where cumulative balance becomes positive).
     *
     * @param cumulativeBalances List of cumulative balances
     * @return The first CumulativeBalance that is non-negative, or null if none found
     */
    public CumulativeBalance findBreakEvenPoint(List<CumulativeBalance> cumulativeBalances) {
        if (cumulativeBalances == null || cumulativeBalances.isEmpty()) {
            return null;
        }

        return cumulativeBalances.stream()
                .filter(cb -> !cb.isDeficit())
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds the worst cumulative balance point (lowest cumulative balance).
     *
     * @param cumulativeBalances List of cumulative balances
     * @return The CumulativeBalance with lowest cumulative amount, or null if empty
     */
    public CumulativeBalance findWorstPoint(List<CumulativeBalance> cumulativeBalances) {
        if (cumulativeBalances == null || cumulativeBalances.isEmpty()) {
            return null;
        }

        return cumulativeBalances.stream()
                .min((a, b) -> a.getCumulativeBalance().compareTo(b.getCumulativeBalance())) // ✅ Balance.compareTo
                .orElse(null);
    }

    /**
     * Validates that all monthly balances are valid and use the same currency.
     */
    private void validateMonthlyBalances(List<MonthlyBalance> monthlyBalances) {
        if (monthlyBalances.stream().anyMatch(mb -> mb == null)) {
            throw new IllegalArgumentException("Monthly balances list contains null elements");
        }

        if (monthlyBalances.size() > 1) {
            String currency = monthlyBalances.get(0).getIncome().getCurrencyCode();
            boolean allSameCurrency = monthlyBalances.stream()
                    .allMatch(mb ->
                            mb.getIncome().getCurrencyCode().equals(currency) &&
                                    mb.getExpense().getCurrencyCode().equals(currency)
                    );

            if (!allSameCurrency) {
                throw new IllegalArgumentException(
                        "All monthly balances must use the same currency"
                );
            }
        }
    }
}