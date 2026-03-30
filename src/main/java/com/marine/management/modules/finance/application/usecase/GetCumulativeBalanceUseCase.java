package com.marine.management.modules.finance.application.usecase;

import com.marine.management.modules.finance.application.dto.CumulativeBalanceDTO;
import com.marine.management.modules.finance.application.mapper.DashboardMapper;
import com.marine.management.modules.finance.application.mapper.MonthlyBalanceMapper;
import com.marine.management.modules.finance.domain.enums.EntryStatus;
import com.marine.management.modules.finance.domain.service.CumulativeBalanceCalculator;
import com.marine.management.modules.finance.domain.vo.CumulativeBalance;
import com.marine.management.modules.finance.domain.vo.MonthlyBalance;
import com.marine.management.modules.finance.domain.vo.Period;
import com.marine.management.modules.finance.infrastructure.FinancialEntryReportRepository;
import com.marine.management.modules.finance.infrastructure.FinancialEntryReportRepository.MonthlyTotalProjection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

/**
 * Use Case: Get Cumulative Balance Trend
 *
 * Orchestrates the calculation of cumulative balance over a time period.
 *
 * <p>Flow:
 * <ol>
 *   <li>Query monthly totals from database (Infrastructure)</li>
 *   <li>Map to domain objects (Application)</li>
 *   <li>Calculate cumulative balance (Domain)</li>
 *   <li>Map to DTOs (Application)</li>
 * </ol>
 */
@Service
@Transactional(readOnly = true)
public class GetCumulativeBalanceUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetCumulativeBalanceUseCase.class);
    private static final String DEFAULT_CURRENCY = "EUR";

    private final FinancialEntryReportRepository reportRepository;
    private final MonthlyBalanceMapper monthlyBalanceMapper;
    private final CumulativeBalanceCalculator cumulativeBalanceCalculator;
    private final DashboardMapper dashboardMapper;

    public GetCumulativeBalanceUseCase(
            FinancialEntryReportRepository reportRepository,
            MonthlyBalanceMapper monthlyBalanceMapper,
            CumulativeBalanceCalculator cumulativeBalanceCalculator,
            DashboardMapper dashboardMapper
    ) {
        this.reportRepository = Objects.requireNonNull(reportRepository);
        this.monthlyBalanceMapper = Objects.requireNonNull(monthlyBalanceMapper);
        this.cumulativeBalanceCalculator = Objects.requireNonNull(cumulativeBalanceCalculator);
        this.dashboardMapper = Objects.requireNonNull(dashboardMapper);
    }

    /**
     * Executes the use case.
     *
     * @param period Time period for calculation (not null)
     * @return List of cumulative balance DTOs
     * @throws NullPointerException if period is null
     */
    public List<CumulativeBalanceDTO> execute(Period period) {
        Objects.requireNonNull(period, "Period cannot be null");

        log.info("Executing GetCumulativeBalanceUseCase for period: {}", period.format());

        // Query monthly totals
        List<MonthlyTotalProjection> projections =
                reportRepository.findMonthlyTotals(period.startDate(), period.endDate(), EntryStatus.ACTUAL_STATUSES);

        if (projections.isEmpty()) {
            log.warn("No data found for period: {}", period.format());
            return List.of();
        }

        // Map to domain objects
        List<MonthlyBalance> monthlyBalances =
                monthlyBalanceMapper.toDomain(projections);

        // Calculate cumulative balance
        List<CumulativeBalance> cumulativeBalances =
                cumulativeBalanceCalculator.calculate(monthlyBalances);

        // Map to DTOs
        List<CumulativeBalanceDTO> result = dashboardMapper.toDTOList(cumulativeBalances);

        log.info("Successfully calculated {} cumulative balance records for {} days",
                result.size(), period.getDaysCount());

        logCriticalLevels(cumulativeBalances);

        return result;
    }

    /**
     * Executes with missing months filled.
     *
     * Ensures all months in the range have data (fills gaps with zero balances).
     * Useful for continuous chart rendering.
     *
     * @param period Time period for calculation (not null)
     * @return Complete list of cumulative balance DTOs (no gaps)
     * @throws NullPointerException if period is null
     */
    public List<CumulativeBalanceDTO> executeWithCompletePeriod(Period period) {
        Objects.requireNonNull(period, "Period cannot be null");

        log.info("Executing GetCumulativeBalanceUseCase with complete period: {}",
                period.formatDetailed());

        // Query monthly totals
        List<MonthlyTotalProjection> projections =
                reportRepository.findMonthlyTotals(period.startDate(), period.endDate(), EntryStatus.ACTUAL_STATUSES);

        // Map to domain objects
        List<MonthlyBalance> monthlyBalances =
                monthlyBalanceMapper.toDomain(projections);

        // Fill missing months
        YearMonth startMonth = YearMonth.from(period.startDate());
        YearMonth endMonth = YearMonth.from(period.endDate());
        List<MonthlyBalance> completeMonthlyBalances =
                monthlyBalanceMapper.fillMissingMonths(
                        monthlyBalances, startMonth, endMonth, DEFAULT_CURRENCY);

        // Calculate cumulative balance
        List<CumulativeBalance> cumulativeBalances =
                cumulativeBalanceCalculator.calculate(completeMonthlyBalances);

        // Map to DTOs
        List<CumulativeBalanceDTO> result = dashboardMapper.toDTOList(cumulativeBalances);

        log.info("Successfully calculated {} complete cumulative balance records (gaps filled) " +
                        "for period: {}",
                result.size(), period.format());

        logCriticalLevels(cumulativeBalances);

        return result;
    }

    /**
     * Logs warnings for critical balance levels.
     */
    private void logCriticalLevels(List<CumulativeBalance> cumulativeBalances) {
        long criticalCount = cumulativeBalances.stream()
                .filter(CumulativeBalance::isCritical)
                .count();

        long warningCount = cumulativeBalances.stream()
                .filter(cb -> cb.isWarning() && !cb.isCritical())
                .count();

        if (criticalCount > 0) {
            log.warn("⚠️ CRITICAL: {} month(s) have critical balance levels (< -10K)", criticalCount);

            CumulativeBalance worst = cumulativeBalanceCalculator.findWorstPoint(cumulativeBalances);
            if (worst != null) {
                log.warn("⚠️ Worst point: {} at {}",
                        worst.getCumulativeBalance(), worst.getMonth());
            }
        }

        if (warningCount > 0) {
            log.warn("⚠️ WARNING: {} month(s) have warning balance levels (< -5K)", warningCount);
        }

        CumulativeBalance breakEven = cumulativeBalanceCalculator.findBreakEvenPoint(cumulativeBalances);
        if (breakEven != null) {
            log.info("✅ Break-even point reached at: {}", breakEven.getMonth());
        }
    }
}