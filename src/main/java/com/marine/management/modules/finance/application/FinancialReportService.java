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
import java.util.UUID;

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
        return getDashboardSummary(period, null);
    }

    /**
     * Gets dashboard summary for a period, optionally filtered by crew member.
     * When crewMemberId is null, returns full organization data (manager/captain view).
     * When crewMemberId is set, returns only that crew member's entries.
     *
     * @param period       Time period for summary
     * @param crewMemberId Optional crew member UUID filter (null = no filter)
     * @return Dashboard summary with totals and counts
     */
    public DashboardSummary getDashboardSummary(Period period, UUID crewMemberId) {
        BigDecimal totalIncome = getActualTotalForCrew(RecordType.INCOME, period, crewMemberId);
        BigDecimal totalExpense = getActualTotalForCrew(RecordType.EXPENSE, period, crewMemberId);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        long incomeCount = countActualByTypeForCrew(RecordType.INCOME, period, crewMemberId);
        long expenseCount = countActualByTypeForCrew(RecordType.EXPENSE, period, crewMemberId);

        // Status-based counts (period-filtered, crew-aware)
        long paidCount = countByStatusesForCrew(Set.of(EntryStatus.PAID), period, crewMemberId);
        long approvedCount = countByStatusesForCrew(Set.of(EntryStatus.APPROVED, EntryStatus.PARTIALLY_PAID), period, crewMemberId);
        long draftCount = countByStatusesForCrew(Set.of(EntryStatus.DRAFT), period, crewMemberId);
        long pendingCount = countByStatusesForCrew(Set.of(EntryStatus.PENDING_CAPTAIN, EntryStatus.PENDING_MANAGER), period, crewMemberId);

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

    /**
     * Count entries with given statuses within period, optionally filtered by crew member.
     */
    private long countByStatusesForCrew(Set<EntryStatus> statuses, Period period, UUID crewMemberId) {
        if (crewMemberId == null) {
            return countByStatuses(statuses, period);
        }
        return entryRepository.countByStatusInAndEntryDateBetweenAndCrewMember(
                statuses,
                period.startDate(),
                period.endDate(),
                crewMemberId
        );
    }

    // ============================================
    // PERIOD QUERIES
    // ============================================

    public List<FinancialEntryReportRepository.PeriodTotalProjection> getPeriodTotals(Period period) {
        return reportRepository.findPeriodTotals(period.startDate(), period.endDate(), EntryStatus.ACTUAL_STATUSES);
    }

    public List<FinancialEntryReportRepository.PeriodTotalProjection> getPeriodTotals(Period period, UUID crewMemberId) {
        if (crewMemberId == null) return getPeriodTotals(period);
        return reportRepository.findPeriodTotalsForCrew(period.startDate(), period.endDate(), EntryStatus.ACTUAL_STATUSES, crewMemberId);
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

    public List<FinancialEntryReportRepository.CategoryTotalProjection> getExpenseTotals(Period period, UUID crewMemberId) {
        if (crewMemberId == null) return getExpenseTotals(period);
        return reportRepository.findExpenseTotalsForCrew(period.startDate(), period.endDate(), EntryStatus.ACTUAL_STATUSES, crewMemberId);
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
     * Total amount filtered by crew member. When crewMemberId is null, returns all.
     */
    public BigDecimal getActualTotalForCrew(RecordType entryType, Period period, UUID crewMemberId) {
        if (crewMemberId == null) return getActualTotal(entryType, period);
        return reportRepository.sumByEntryTypeAndDateRangeForCrew(
                entryType,
                period.startDate(),
                period.endDate(),
                EntryStatus.ACTUAL_STATUSES,
                crewMemberId
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

    /**
     * Count of approved/paid entries filtered by crew member.
     */
    public long countActualByTypeForCrew(RecordType entryType, Period period, UUID crewMemberId) {
        if (crewMemberId == null) return countActualByType(entryType, period);
        return entryRepository.countByStatusInAndEntryDateBetweenAndCrewMember(
                EntryStatus.ACTUAL_STATUSES,
                period.startDate(),
                period.endDate(),
                crewMemberId
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

    public List<FinancialEntryReportRepository.MonthlyIncomeExpenseProjection> getMonthlyIncomeExpense(
            int year, UUID crewMemberId
    ) {
        if (crewMemberId == null) return getMonthlyIncomeExpense(year);
        return reportRepository.findMonthlyIncomeExpenseForCrew(year, EntryStatus.ACTUAL_STATUSES, crewMemberId);
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
