package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.application.dto.PivotTreeReportResponse;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.model.PivotReportProjection;
import com.marine.management.modules.finance.domain.service.PivotReportBuilder;
import com.marine.management.modules.finance.domain.service.TreeReportBuilder;
import com.marine.management.modules.finance.domain.vo.Period;
import com.marine.management.modules.finance.infrastructure.FinancialEntryRepository;
import com.marine.management.modules.finance.infrastructure.FinancialEntryReportRepository;
import com.marine.management.modules.finance.presentation.dto.reports.DashboardSummary;
import com.marine.management.modules.finance.presentation.dto.reports.PivotReportRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Application Service: Financial Report Service
 *
 * Orchestrates financial reporting queries and aggregations.
 * Delegates to repositories for data access and domain services for calculations.
 */
@Service
@Transactional(readOnly = true)
public class FinancialReportService {

    private final FinancialEntryRepository entryRepository;
    private final FinancialEntryReportRepository reportRepository;
    private final TreeReportBuilder treeBuilder;
    private final PivotReportBuilder pivotBuilder;

    public FinancialReportService(
            FinancialEntryRepository entryRepository,
            FinancialEntryReportRepository reportRepository,
            TreeReportBuilder treeBuilder,
            PivotReportBuilder pivotBuilder
    ) {
        this.entryRepository = entryRepository;
        this.reportRepository = reportRepository;
        this.treeBuilder = treeBuilder;
        this.pivotBuilder = pivotBuilder;
    }

    // ============================================
    // DASHBOARD SUMMARY
    // ============================================

    /**
     * Gets dashboard summary for a period.
     *
     * @param period Time period for summary
     * @return Dashboard summary with totals and counts
     */
    public DashboardSummary getDashboardSummary(Period period) {
        BigDecimal totalIncome = getTotalByType(RecordType.INCOME, period);
        BigDecimal totalExpense = getTotalByType(RecordType.EXPENSE, period);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        long incomeCount = countByType(RecordType.INCOME, period);
        long expenseCount = countByType(RecordType.EXPENSE, period);

        return new DashboardSummary(totalIncome, totalExpense, balance, incomeCount, expenseCount);
    }

    // ============================================
    // PERIOD QUERIES
    // ============================================

    public List<FinancialEntryReportRepository.PeriodTotalProjection> getPeriodTotals(Period period) {
        return reportRepository.findPeriodTotals(period.startDate(), period.endDate());
    }

    public List<FinancialEntryReportRepository.CategoryTotalProjection> getCategoryTotals(
            RecordType entryType,
            Period period
    ) {
        return reportRepository.findCategoryTotals(entryType, period.startDate(), period.endDate());
    }

    public List<FinancialEntryReportRepository.CategoryTotalProjection> getExpenseTotals(Period period) {
        return reportRepository.findExpenseTotals(period.startDate(), period.endDate());
    }

    public List<FinancialEntryReportRepository.CategoryTotalProjection> getIncomeTotals(Period period) {
        return reportRepository.findIncomeTotals(period.startDate(), period.endDate());
    }

    public List<FinancialEntryReportRepository.MonthlyTotalProjection> getMonthlyTotals(Period period) {
        return reportRepository.findMonthlyTotals(period.startDate(), period.endDate());
    }

    // ============================================
    // AGGREGATIONS
    // ============================================

    public BigDecimal getTotalByType(RecordType entryType, Period period) {
        return reportRepository.sumByEntryTypeAndDateRange(
                entryType,
                period.startDate(),
                period.endDate()
        );
    }

    public long countByType(RecordType entryType, Period period) {
        return entryRepository.countByEntryTypeAndEntryDateBetween(
                entryType,
                period.startDate(),
                period.endDate()
        );
    }

    // ============================================
    // YEARLY QUERIES (no Period needed - already year-based)
    // ============================================

    public List<FinancialEntryReportRepository.CategoryMonthBreakdownProjection> getCategoryMonthBreakdown(
            RecordType entryType,
            int year
    ) {
        return reportRepository.findCategoryMonthBreakdown(entryType, year);
    }

    public List<FinancialEntryReportRepository.MonthlyIncomeExpenseProjection> getMonthlyIncomeExpense(
            int year
    ) {
        return reportRepository.findMonthlyIncomeExpense(year);
    }

    // ============================================
    // PIVOT REPORTS
    // ============================================

    public PivotTreeReportResponse generatePivotReport(PivotReportRequest request) {
        int year = request.year();

        List<PivotReportProjection> projections = reportRepository.findPivotProjections(
                RecordType.EXPENSE,
                year
        );

        return pivotBuilder.buildPivotReport(year, request.currency(), projections);
    }
}