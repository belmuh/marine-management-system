package com.marine.management.modules.finance.application.usecase;

import com.marine.management.modules.finance.application.mapper.PeriodReportMapper;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.model.MonthlyBreakdown;
import com.marine.management.modules.finance.domain.model.MonthlyTotal;
import com.marine.management.modules.finance.domain.model.PeriodReport;
import com.marine.management.modules.finance.domain.vo.Period;
import com.marine.management.modules.finance.infrastructure.FinancialEntryReportRepository;
import com.marine.management.modules.finance.presentation.dto.reports.PeriodBreakdownDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Use Case: Generate Period Report
 *
 * Produces breakdown of expenses by category and month for a custom date range.
 *
 * <p>Flow:
 * <ol>
 *   <li>Fetch carry-over balance (Infrastructure)</li>
 *   <li>Fetch category breakdowns from database (Infrastructure)</li>
 *   <li>Fetch monthly income/expense totals (Infrastructure)</li>
 *   <li>Build domain model (Domain)</li>
 *   <li>Map to DTO (Application)</li>
 * </ol>
 */
@Service
@Transactional(readOnly = true)
public class GeneratePeriodReportUseCase {

    private static final String DEFAULT_CURRENCY = "EUR"; // TODO: Get from tenant context

    private final FinancialEntryReportRepository reportRepository;
    private final PeriodReportMapper periodReportMapper;

    public GeneratePeriodReportUseCase(
            FinancialEntryReportRepository reportRepository,
            PeriodReportMapper periodReportMapper
    ) {
        this.reportRepository = Objects.requireNonNull(reportRepository);
        this.periodReportMapper = Objects.requireNonNull(periodReportMapper);
    }

    /**
     * Generates period report for the given date range.
     *
     * @param startDate Start date of the period (not null)
     * @param endDate   End date of the period (not null)
     * @return Period breakdown DTO with monthly data
     * @throws NullPointerException     if any parameter is null
     * @throws IllegalArgumentException if period is invalid
     */
    public PeriodBreakdownDto execute(LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(startDate, "Start date cannot be null");
        Objects.requireNonNull(endDate, "End date cannot be null");

        Period period = Period.of(startDate, endDate);

        // Fetch carry-over balance: net of all approved entries before this period
        BigDecimal carryOver = reportRepository.findCarryOverBalance(period.startDate());

        // Fetch data from database
        List<FinancialEntryReportRepository.CategoryMonthBreakdownProjection> categoryBreakdowns =
                reportRepository.findCategoryMonthBreakdownByPeriod(
                        RecordType.EXPENSE, period.startDate(), period.endDate()
                );

        List<FinancialEntryReportRepository.MonthlyTotalProjection> monthlyTotals =
                reportRepository.findMonthlyTotals(period.startDate(), period.endDate());

        // Build domain model
        PeriodReport report = buildPeriodReport(period, categoryBreakdowns, monthlyTotals, carryOver);

        // Map to DTO
        return periodReportMapper.toDto(report);
    }

    /**
     * Builds PeriodReport domain model from infrastructure projections.
     */
    private PeriodReport buildPeriodReport(
            Period period,
            List<FinancialEntryReportRepository.CategoryMonthBreakdownProjection> categoryBreakdowns,
            List<FinancialEntryReportRepository.MonthlyTotalProjection> monthlyTotals,
            BigDecimal carryOver
    ) {
        List<MonthlyBreakdown> breakdowns = buildCategoryBreakdowns(categoryBreakdowns);
        Map<Integer, MonthlyTotal> totalsMap = buildMonthlyTotals(monthlyTotals, carryOver);

        return new PeriodReport(
                period,
                breakdowns,
                totalsMap,
                DEFAULT_CURRENCY,
                carryOver
        );
    }

    /**
     * Builds category breakdowns grouped by month from infrastructure projections.
     */
    private List<MonthlyBreakdown> buildCategoryBreakdowns(
            List<FinancialEntryReportRepository.CategoryMonthBreakdownProjection> projections
    ) {
        Map<String, Map<Integer, BigDecimal>> categoryMonthValues = new HashMap<>();

        for (var projection : projections) {
            String categoryName = projection.getCategoryName();
            Integer month = projection.getMonth();
            BigDecimal amount = projection.getTotal();

            categoryMonthValues
                    .computeIfAbsent(categoryName, k -> new HashMap<>())
                    .put(month, amount);
        }

        return categoryMonthValues.entrySet().stream()
                .map(entry -> {
                    String categoryName = entry.getKey();
                    Map<Integer, BigDecimal> monthlyValues = entry.getValue();
                    BigDecimal periodTotal = sumMonthlyValues(monthlyValues);

                    return new MonthlyBreakdown(categoryName, monthlyValues, periodTotal);
                })
                .sorted(Comparator.comparing(MonthlyBreakdown::categoryName))
                .toList();
    }

    /**
     * Builds monthly totals with cumulative balance.
     *
     * @param carryOver net balance of all approved entries before this period
     */
    private Map<Integer, MonthlyTotal> buildMonthlyTotals(
            List<FinancialEntryReportRepository.MonthlyTotalProjection> projections,
            BigDecimal carryOver
    ) {
        Map<Integer, BigDecimal> incomeByMonth = new HashMap<>();
        Map<Integer, BigDecimal> expenseByMonth = new HashMap<>();

        for (var projection : projections) {
            Integer month = projection.getMonth();
            BigDecimal total = projection.getTotal();

            if (projection.getEntryType() == RecordType.INCOME) {
                incomeByMonth.put(month, total);
            } else {
                expenseByMonth.put(month, total);
            }
        }

        return buildMonthlyTotalsMap(incomeByMonth, expenseByMonth, carryOver);
    }

    /**
     * Calculates cumulative balance for each month, starting from carry-over balance.
     * carryOver = net income - expense for all approved entries before this period.
     */
    private Map<Integer, MonthlyTotal> buildMonthlyTotalsMap(
            Map<Integer, BigDecimal> incomeByMonth,
            Map<Integer, BigDecimal> expenseByMonth,
            BigDecimal carryOver
    ) {
        Map<Integer, MonthlyTotal> totals = new HashMap<>();
        BigDecimal cumulative = carryOver;

        for (int month = 1; month <= 12; month++) {
            BigDecimal income = incomeByMonth.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal expense = expenseByMonth.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal net = income.subtract(expense);
            cumulative = cumulative.add(net);

            totals.put(month, new MonthlyTotal(income, expense, cumulative));
        }

        return totals;
    }

    private BigDecimal sumMonthlyValues(Map<Integer, BigDecimal> monthlyValues) {
        return monthlyValues.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
