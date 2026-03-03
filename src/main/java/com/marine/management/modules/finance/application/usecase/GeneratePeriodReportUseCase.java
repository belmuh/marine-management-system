package com.marine.management.modules.finance.application.usecase;

import com.marine.management.modules.finance.application.mapper.PeriodReportMapper;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.model.MonthlyBreakdown;
import com.marine.management.modules.finance.domain.model.MonthlyTotal;
import com.marine.management.modules.finance.domain.model.PeriodReport;
import com.marine.management.modules.finance.domain.vo.Period;
import com.marine.management.modules.finance.infrastructure.FinancialEntryRepository;
import com.marine.management.modules.finance.infrastructure.FinancialEntryRepository.FinancialEntryProjection;
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
 *   <li>Fetch financial entries for period (Infrastructure)</li>
 *   <li>Calculate category breakdowns (Application)</li>
 *   <li>Calculate monthly totals (Application)</li>
 *   <li>Build domain model (Domain)</li>
 *   <li>Map to DTO (Application)</li>
 * </ol>
 */
@Service
@Transactional(readOnly = true)
public class GeneratePeriodReportUseCase {

    private static final String DEFAULT_CURRENCY = "EUR"; // TODO: Get from tenant context

    private final FinancialEntryRepository repository;
    private final PeriodReportMapper periodReportMapper;

    public GeneratePeriodReportUseCase(
            FinancialEntryRepository repository,
            PeriodReportMapper periodReportMapper
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.periodReportMapper = Objects.requireNonNull(periodReportMapper);
    }

    /**
     * Generates period report for the given date range.
     *
     * @param startDate Start date of the period (not null)
     * @param endDate End date of the period (not null)
     * @return Period breakdown DTO with monthly data
     * @throws NullPointerException if any parameter is null
     * @throws IllegalArgumentException if period is invalid
     */
    public PeriodBreakdownDto execute(LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(startDate, "Start date cannot be null");
        Objects.requireNonNull(endDate, "End date cannot be null");

        Period period = Period.of(startDate, endDate);

        // Fetch financial entries
        List<FinancialEntryProjection> entries = repository.findByEntryDateBetweenOrderByEntryDateDesc(
                period.startDate(),
                period.endDate()
        );

        // Calculate breakdowns and totals
        List<MonthlyBreakdown> categoryBreakdowns = calculateCategoryBreakdowns(entries);
        Map<Integer, MonthlyTotal> monthlyTotals = calculateMonthlyTotals(entries);

        // Build domain model
        PeriodReport report = new PeriodReport(
                period,
                categoryBreakdowns,
                monthlyTotals,
                DEFAULT_CURRENCY
        );

        // Map to DTO
        return periodReportMapper.toDto(report);
    }

    /**
     * Calculates category breakdowns grouped by month.
     */
    private List<MonthlyBreakdown> calculateCategoryBreakdowns(
            List<FinancialEntryProjection> entries
    ) {
        Map<String, Map<Integer, BigDecimal>> categoryMonthTotals = new HashMap<>();

        for (FinancialEntryProjection entry : entries) {
            String category = entry.getCategoryName();
            Integer month = entry.getEntryDate().getMonthValue();
            BigDecimal amount = entry.getBaseAmount();

            categoryMonthTotals
                    .computeIfAbsent(category, k -> new HashMap<>())
                    .merge(month, amount, BigDecimal::add);
        }

        return categoryMonthTotals.entrySet().stream()
                .map(e -> {
                    String categoryName = e.getKey();
                    Map<Integer, BigDecimal> monthlyValues = e.getValue();
                    BigDecimal yearTotal = sumMonthlyValues(monthlyValues);

                    return new MonthlyBreakdown(categoryName, monthlyValues, yearTotal);
                })
                .sorted(Comparator.comparing(MonthlyBreakdown::categoryName))
                .toList();
    }

    /**
     * Calculates monthly totals with cumulative balance.
     */
    private Map<Integer, MonthlyTotal> calculateMonthlyTotals(
            List<FinancialEntryProjection> entries
    ) {
        Map<Integer, BigDecimal> incomeByMonth = new HashMap<>();
        Map<Integer, BigDecimal> expenseByMonth = new HashMap<>();

        for (FinancialEntryProjection entry : entries) {
            Integer month = entry.getEntryDate().getMonthValue();
            BigDecimal amount = entry.getBaseAmount();

            if (entry.getEntryType() == RecordType.INCOME) {
                incomeByMonth.merge(month, amount, BigDecimal::add);
            } else {
                expenseByMonth.merge(month, amount, BigDecimal::add);
            }
        }

        return buildMonthlyTotalsMap(incomeByMonth, expenseByMonth);
    }

    /**
     * Builds monthly totals map with cumulative balance.
     */
    private Map<Integer, MonthlyTotal> buildMonthlyTotalsMap(
            Map<Integer, BigDecimal> incomeByMonth,
            Map<Integer, BigDecimal> expenseByMonth
    ) {
        Map<Integer, MonthlyTotal> totals = new HashMap<>();
        BigDecimal cumulative = BigDecimal.ZERO;

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