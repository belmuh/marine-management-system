package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.application.dto.PivotTreeReportResponse;
import com.marine.management.modules.finance.domain.enums.EntryStatus;
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
import java.util.Set;

/**
 * Application Service: Financial Report Service
 *
 * Orchestrates financial reporting queries and aggregations.
 * Delegates to repositories for data access and domain services for calculations.
 *
 * Status filtering strategy:
 *   - All financial totals use EntryStatus.ACTUAL_STATUSES (APPROVED, PARTIALLY_PAID, PAID)
 *   - Committed/forecast totals use EntryStatus.COMMITTED_STATUSES (PENDING_CAPTAIN, PENDING_MANAGER)
 *   - DRAFT and REJECTED are never included in any financial calculation
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
     * Financial totals include only ACTUAL entries (approved / paid).
     * Committed totals show pending entries separately for forecast display.
     *
     * @param period Time period for summary
     * @return Dashboard summary with totals and counts
     */
    public DashboardSummary getDashboardSummary(Period period) {
        BigDecimal totalIncome = getActualTotal(RecordType.INCOME, period);
        BigDecimal totalExpense = getActualTotal(RecordType.EXPENSE, period);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        long incomeCount = countActualByType(RecordType.INCOME, period);
        long expenseCount = countActualByType(RecordType.EXPENSE, period);

        // Status-based counts (period-filtered)
        long paidCount = countByStatuses(Set.of(EntryStatus.PAID), period);
        long approvedCount = countByStatuses(Set.of(EntryStatus.APPROVED, EntryStatus.PARTIALLY_PAID), period);
        long draftCount = countByStatuses(Set.of(EntryStatus.DRAFT), period);
        long pendingCount = countByStatuses(Set.of(EntryStatus.PENDING_CAPTAIN, EntryStatus.PENDING_MANAGER), period);

        return new DashboardSummary(totalIncome, totalExpense, balance, incomeCount, expenseCount,
                paidCount, approvedCount, draftCount, pendingCount);
    }

    /**
     * Count entries with given statuses within period.
     */
    private long countByStatuses(Set<EntryStatus> statuses, Period period) {
        return entryRepository.countByStatusInAndEntryDateBetween(
                statuses,
                period.startDate(),
                period.endDate()
        );
    }

    // ============================================
    // PERIOD QUERIES
    // ============================================

    public List<FinancialEntryReportRepository.PeriodTotalProjection> getPeriodTotals(Period period) {
        return reportRepository.findPeriodTotals(period.startDate(), period.endDate(), EntryStatus.ACTUAL_STATUSES);
    }

    public List<FinancialEntryReportRepository.CategoryTotalProjection> getCategoryTotals(
            RecordType entryType,
            Period period
    ) {
        return reportRepository.findCategoryTotals(entryType, period.startDate(), period.endDate(), EntryStatus.ACTUAL_STATUSES);
    }

    public List<FinancialEntryReportRepository.CategoryTotalProjection> getExpenseTotals(Period period) {
        return reportRepository.findExpenseTotals(period.startDate(), period.endDate(), EntryStatus.ACTUAL_STATUSES);
    }

    public List<FinancialEntryReportRepository.CategoryTotalProjection> getIncomeTotals(Period period) {
        return reportRepository.findIncomeTotals(period.startDate(), period.endDate(), EntryStatus.ACTUAL_STATUSES);
    }

    public List<FinancialEntryReportRepository.MonthlyTotalProjection> getMonthlyTotals(Period period) {
        return reportRepository.findMonthlyTotals(period.startDate(), period.endDate(), EntryStatus.ACTUAL_STATUSES);
    }

    // ============================================
    // AGGREGATIONS — ACTUAL (finansal gerçekler)
    // ============================================

    /**
     * Total amount for approved/paid entries of a given type in a period.
     */
    public BigDecimal getActualTotal(RecordType entryType, Period period) {
        return reportRepository.sumByEntryTypeAndDateRange(
                entryType,
                period.startDate(),
                period.endDate(),
                EntryStatus.ACTUAL_STATUSES
        );
    }

    /**
     * Count of approved/paid entries of a given type in a period.
     * Consistent with getActualTotal() — only counts the same entries.
     */
    public long countActualByType(RecordType entryType, Period period) {
        return entryRepository.countByStatusInAndEntryDateBetween(
                EntryStatus.ACTUAL_STATUSES,
                period.startDate(),
                period.endDate()
        );
    }

    // ============================================
    // AGGREGATIONS — COMMITTED (onay bekleyen / forecast)
    // ============================================

    /**
     * Total amount for pending-approval entries of a given type in a period.
     * Used for forecast/pipeline display on dashboard — shown separately from actuals.
     */
    public BigDecimal getCommittedTotal(RecordType entryType, Period period) {
        return reportRepository.sumByEntryTypeAndDateRange(
                entryType,
                period.startDate(),
                period.endDate(),
                EntryStatus.COMMITTED_STATUSES
        );
    }

    /**
     * Carry-over balance: net approved balance before the given period.
     * Used as the starting cumulative balance for annual/period reports.
     */
    public BigDecimal getCarryOverBalance(Period period) {
        return reportRepository.findCarryOverBalance(period.startDate(), EntryStatus.ACTUAL_STATUSES);
    }

    // ============================================
    // YEARLY QUERIES (no Period needed - already year-based)
    // ============================================

    public List<FinancialEntryReportRepository.CategoryMonthBreakdownProjection> getCategoryMonthBreakdown(
            RecordType entryType,
            int year
    ) {
        return reportRepository.findCategoryMonthBreakdown(entryType, year, EntryStatus.ACTUAL_STATUSES);
    }

    public List<FinancialEntryReportRepository.MonthlyIncomeExpenseProjection> getMonthlyIncomeExpense(
            int year
    ) {
        return reportRepository.findMonthlyIncomeExpense(year, EntryStatus.ACTUAL_STATUSES);
    }

    // ============================================
    // PIVOT REPORTS
    // ============================================

    public PivotTreeReportResponse generatePivotReport(PivotReportRequest request) {
        int year = request.year();

        List<PivotReportProjection> projections = reportRepository.findPivotProjections(
                RecordType.EXPENSE,
                year,
                EntryStatus.ACTUAL_STATUSES
        );

        return pivotBuilder.buildPivotReport(year, request.currency(), projections);
    }
}
