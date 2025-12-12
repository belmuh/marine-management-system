package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.model.AnnualReport;
import com.marine.management.modules.finance.domain.vo.Period;
import com.marine.management.modules.finance.domain.model.PeriodReport;
import com.marine.management.modules.finance.domain.service.ReportGenerator;
import com.marine.management.modules.finance.infrastructure.FinancialEntryRepository;
import com.marine.management.modules.finance.presentation.dto.reports.DashboardSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Application service for financial reporting and analytics.
 * Handles dashboard summaries, period reports, and statistical queries.
 */
@Service
@Transactional(readOnly = true)
public class FinancialReportService {

    private final FinancialEntryRepository entryRepository;
    private final ReportGenerator reportGenerator;

    public FinancialReportService(
            FinancialEntryRepository entryRepository,
            ReportGenerator reportGenerator
    ) {
        this.entryRepository = entryRepository;
        this.reportGenerator = reportGenerator;
    }

    // ============================================
    // DASHBOARD SUMMARY
    // ============================================

    /**
     * Get dashboard summary with totals and counts for a date range
     */
    public DashboardSummary getDashboardSummary(LocalDate startDate, LocalDate endDate) {
        BigDecimal totalIncome = getTotalByType(RecordType.INCOME, startDate, endDate);
        BigDecimal totalExpense = getTotalByType(RecordType.EXPENSE, startDate, endDate);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        long incomeCount = countByType(RecordType.INCOME, startDate, endDate);
        long expenseCount = countByType(RecordType.EXPENSE, startDate, endDate);

        return new DashboardSummary(
                totalIncome,
                totalExpense,
                balance,
                incomeCount,
                expenseCount
        );
    }

    // ============================================
    // PERIOD REPORTS
    // ============================================

    /**
     * Get period totals (income/expense breakdown)
     */
    public List<FinancialEntryRepository.PeriodTotalProjection> getPeriodTotals(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return entryRepository.findPeriodTotals(startDate, endDate);
    }

    /**
     * Get category totals for a specific entry type and date range
     */
    public List<FinancialEntryRepository.CategoryTotalProjection> getCategoryTotals(
            RecordType entryType,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return entryRepository.findCategoryTotals(entryType, startDate, endDate);
    }

    /**
     * Get expense totals by category
     */
    public List<FinancialEntryRepository.CategoryTotalProjection> getExpenseTotals(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return entryRepository.findExpenseTotals(startDate, endDate);
    }

    /**
     * Get income totals by category
     */
    public List<FinancialEntryRepository.CategoryTotalProjection> getIncomeTotals(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return entryRepository.findIncomeTotals(startDate, endDate);
    }

    /**
     * Get monthly totals for charts and trend analysis
     */
    public List<FinancialEntryRepository.MonthlyTotalProjection> getMonthlyTotals(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return entryRepository.findMonthlyTotals(startDate, endDate);
    }

    // ============================================
    // ANNUAL & PERIOD REPORTS
    // ============================================

    /**
     * Generate comprehensive annual report with category breakdowns
     */
    public AnnualReport generateAnnualReport(int year) {
        var entries = entryRepository.findByEntryDateBetweenOrderByEntryDateDesc(
                LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31)
        );
        return reportGenerator.generateAnnualReport(entries, year);
    }

    /**
     * Generate period report for custom date range
     */
    public PeriodReport generatePeriodReport(LocalDate startDate, LocalDate endDate) {
        Period period = Period.of(startDate, endDate);
        var entries = entryRepository.findByEntryDateBetweenOrderByEntryDateDesc(
                startDate,
                endDate
        );
        return reportGenerator.generatePeriodReport(entries, period);
    }

    // ============================================
    // STATISTICS
    // ============================================

    /**
     * Get total amount by entry type for a date range
     */
    public BigDecimal getTotalByType(
            RecordType entryType,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return entryRepository.sumByEntryTypeAndDateRange(entryType, startDate, endDate);
    }

    /**
     * Count entries by type for a date range
     */
    public long countByType(
            RecordType entryType,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return entryRepository.countByEntryTypeAndEntryDateBetween(entryType, startDate, endDate);
    }

    /**
     * Get category breakdown by month for a specific year
     */
    public List<FinancialEntryRepository.CategoryMonthBreakdownProjection> getCategoryMonthBreakdown(
            RecordType entryType,
            int year
    ) {
        return entryRepository.findCategoryMonthBreakdown(entryType, year);
    }

    /**
     * Get monthly income/expense totals for a year
     */
    public List<FinancialEntryRepository.MonthlyIncomeExpenseProjection> getMonthlyIncomeExpense(
            int year
    ) {
        return entryRepository.findMonthlyIncomeExpense(year);
    }
}