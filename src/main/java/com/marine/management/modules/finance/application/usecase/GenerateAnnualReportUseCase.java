package com.marine.management.modules.finance.application.usecase;

import com.marine.management.modules.finance.application.mapper.AnnualReportMapper;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.model.AnnualReport;
import com.marine.management.modules.finance.domain.model.CategoryYearSummary;
import com.marine.management.modules.finance.domain.model.MonthlyTotal;
import com.marine.management.modules.finance.infrastructure.FinancialEntryReportRepository;
import com.marine.management.modules.finance.presentation.dto.reports.AnnualBreakdownDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Use Case: Generate Annual Report
 *
 * Produces yearly breakdown of expenses by category and month.
 *
 * <p>Flow:
 * <ol>
 *   <li>Fetch category breakdowns from database (Infrastructure)</li>
 *   <li>Fetch monthly income/expense totals (Infrastructure)</li>
 *   <li>Build domain model (Domain)</li>
 *   <li>Map to DTO (Application)</li>
 * </ol>
 */
@Service
@Transactional(readOnly = true)
public class GenerateAnnualReportUseCase {

    private static final int MIN_YEAR = 2000;
    private static final String DEFAULT_CURRENCY = "EUR"; // TODO: Get from tenant context

    private final FinancialEntryReportRepository reportRepository;
    private final AnnualReportMapper annualReportMapper;

    public GenerateAnnualReportUseCase(
            FinancialEntryReportRepository reportRepository,
            AnnualReportMapper annualReportMapper
    ) {
        this.reportRepository = Objects.requireNonNull(reportRepository);
        this.annualReportMapper = Objects.requireNonNull(annualReportMapper);
    }

    /**
     * Generates annual expense report for the given year.
     *
     * @param year Year to generate report for (2000 to current year)
     * @return Annual breakdown DTO with monthly data
     * @throws IllegalArgumentException if year is out of valid range
     */
    public AnnualBreakdownDto execute(int year) {
        validateYear(year);

        // Fetch data from database
        List<FinancialEntryReportRepository.CategoryMonthBreakdownProjection> categoryBreakdowns =
                reportRepository.findCategoryMonthBreakdown(RecordType.EXPENSE, year);

        List<FinancialEntryReportRepository.MonthlyIncomeExpenseProjection> monthlyTotals =
                reportRepository.findMonthlyIncomeExpense(year);

        // Build domain model
        AnnualReport report = buildAnnualReport(year, categoryBreakdowns, monthlyTotals);

        // Map to DTO
        return annualReportMapper.toDto(report);
    }

    /**
     * Builds AnnualReport domain model from infrastructure projections.
     */
    private AnnualReport buildAnnualReport(
            int year,
            List<FinancialEntryReportRepository.CategoryMonthBreakdownProjection> categoryBreakdowns,
            List<FinancialEntryReportRepository.MonthlyIncomeExpenseProjection> monthlyTotals
    ) {
        List<CategoryYearSummary> summaries = buildCategorySummaries(categoryBreakdowns);
        Map<Integer, MonthlyTotal> totalsMap = buildMonthlyTotals(monthlyTotals);

        return new AnnualReport(
                year,
                DEFAULT_CURRENCY,
                totalsMap,
                summaries
        );
    }

    /**
     * Builds category summaries from infrastructure projections.
     */
    private List<CategoryYearSummary> buildCategorySummaries(
            List<FinancialEntryReportRepository.CategoryMonthBreakdownProjection> projections
    ) {
        Map<String, Map<Integer, BigDecimal>> categoryMonthlyValues = new HashMap<>();

        for (var projection : projections) {
            String categoryName = projection.getCategoryName();
            Integer month = projection.getMonth();
            BigDecimal amount = projection.getTotal();

            categoryMonthlyValues
                    .computeIfAbsent(categoryName, k -> new HashMap<>())
                    .put(month, amount);
        }

        return categoryMonthlyValues.entrySet().stream()
                .map(entry -> {
                    String categoryName = entry.getKey();
                    Map<Integer, BigDecimal> monthlyValues = entry.getValue();
                    BigDecimal yearTotal = sumMonthlyValues(monthlyValues);

                    return new CategoryYearSummary(
                            "", // categoryCode not used in current implementation
                            categoryName,
                            monthlyValues,
                            yearTotal
                    );
                })
                .toList();
    }

    /**
     * Builds monthly totals with cumulative balance.
     */
    private Map<Integer, MonthlyTotal> buildMonthlyTotals(
            List<FinancialEntryReportRepository.MonthlyIncomeExpenseProjection> projections
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

        return buildMonthlyTotalsMap(incomeByMonth, expenseByMonth);
    }

    /**
     * Calculates cumulative balance for each month.
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

    private BigDecimal calculateGrandTotal(Map<Integer, MonthlyTotal> totals) {
        return totals.values().stream()
                .map(MonthlyTotal::expense)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateRemainingMoney(Map<Integer, MonthlyTotal> totals) {
        return totals.get(12).cumulative();
    }

    /**
     * Validates year is within acceptable range.
     *
     * @throws IllegalArgumentException if year is out of range
     */
    private void validateYear(int year) {
        int currentYear = java.time.Year.now().getValue();
        if (year < MIN_YEAR || year > currentYear) {
            throw new IllegalArgumentException(
                    String.format("Year must be between %d and %d, got: %d",
                            MIN_YEAR, currentYear, year)
            );
        }
    }
}