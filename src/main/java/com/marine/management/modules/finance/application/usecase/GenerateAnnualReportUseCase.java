package com.marine.management.modules.finance.application.usecase;

import com.marine.management.modules.finance.application.mapper.ReportMapper;
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

@Service
@Transactional(readOnly = true)
public class GenerateAnnualReportUseCase {

    private final FinancialEntryReportRepository reportRepository;
    private final ReportMapper reportMapper;

    public GenerateAnnualReportUseCase(
            FinancialEntryReportRepository reportRepository,
            ReportMapper reportMapper
    ) {
        this.reportRepository = reportRepository;
        this.reportMapper = reportMapper;
    }

    public AnnualBreakdownDto execute(int year) {
        validateYear(year);

        // 1. Fetch category breakdowns from repository
        List<FinancialEntryReportRepository.CategoryMonthBreakdownProjection> categoryBreakdowns =
                reportRepository.findCategoryMonthBreakdown(RecordType.EXPENSE, year);

        // 2. Fetch monthly income/expense
        List<FinancialEntryReportRepository.MonthlyIncomeExpenseProjection> monthlyTotals =
                reportRepository.findMonthlyIncomeExpense(year);

        // 3. Transform to domain model
        AnnualReport report = buildAnnualReport(year, categoryBreakdowns, monthlyTotals);

        // 4. Map to DTO
        return reportMapper.toAnnualBreakdownDto(report);
    }

    private AnnualReport buildAnnualReport(
            int year,
            List<FinancialEntryReportRepository.CategoryMonthBreakdownProjection> categoryBreakdowns,
            List<FinancialEntryReportRepository.MonthlyIncomeExpenseProjection> monthlyTotals
    ) {
        // Convert projections to CategoryYearSummary
        Map<String, CategoryYearSummary> categoryMap = new HashMap<>();

        for (FinancialEntryReportRepository.CategoryMonthBreakdownProjection projection : categoryBreakdowns) {
            String categoryName = projection.getCategoryName();
            Integer month = projection.getMonth();
            BigDecimal amount = projection.getTotal();

            categoryMap.computeIfAbsent(categoryName, name -> {
                Map<Integer, BigDecimal> monthlyValues = new HashMap<>();
                return new CategoryYearSummary("", name, monthlyValues, BigDecimal.ZERO);
            });

            categoryMap.get(categoryName).monthlyValues().put(month, amount);
        }

        // Calculate year totals
        List<CategoryYearSummary> summaries = categoryMap.values().stream()
                .map(summary -> {
                    BigDecimal yearTotal = summary.monthlyValues().values().stream()
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new CategoryYearSummary(
                            summary.categoryCode(),
                            summary.categoryName(),
                            summary.monthlyValues(),
                            yearTotal
                    );
                })
                .toList();

        // Build monthly totals
        Map<Integer, MonthlyTotal> totalsMap = buildMonthlyTotals(monthlyTotals);

        // Calculate grand total and remaining money
        BigDecimal grandTotal = calculateGrandTotal(totalsMap);
        BigDecimal remainingMoney = calculateRemainingMoney(totalsMap);

        return new AnnualReport(
                year,
                "EUR", // TODO: Get from context
                totalsMap,
                summaries,
                grandTotal,
                remainingMoney
        );
    }

    private Map<Integer, MonthlyTotal> buildMonthlyTotals(
            List<FinancialEntryReportRepository.MonthlyIncomeExpenseProjection> projections
    ) {
        Map<Integer, BigDecimal> incomeByMonth = new HashMap<>();
        Map<Integer, BigDecimal> expenseByMonth = new HashMap<>();

        for (FinancialEntryReportRepository.MonthlyIncomeExpenseProjection projection : projections) {
            Integer month = projection.getMonth();
            BigDecimal total = projection.getTotal();

            if (projection.getEntryType() == RecordType.INCOME) {
                incomeByMonth.put(month, total);
            } else {
                expenseByMonth.put(month, total);
            }
        }

        BigDecimal cumulative = BigDecimal.ZERO;
        Map<Integer, MonthlyTotal> totals = new HashMap<>();

        for (int month = 1; month <= 12; month++) {
            BigDecimal income = incomeByMonth.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal expense = expenseByMonth.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal net = income.subtract(expense);
            cumulative = cumulative.add(net);

            totals.put(month, new MonthlyTotal(income, expense, cumulative));
        }

        return totals;
    }

    private BigDecimal calculateGrandTotal(Map<Integer, MonthlyTotal> totals) {
        return totals.values().stream()
                .map(MonthlyTotal::expense)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateRemainingMoney(Map<Integer, MonthlyTotal> totals) {
        // Last month's cumulative
        return totals.get(12).cumulative();
    }

    private void validateYear(int year) {
        int currentYear = java.time.Year.now().getValue();
        if (year < 2000 || year > currentYear) {
            throw new IllegalArgumentException(
                    "Year must be between 2000 and " + currentYear
            );
        }
    }
}