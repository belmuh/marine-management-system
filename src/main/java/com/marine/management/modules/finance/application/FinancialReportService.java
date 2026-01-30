package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.application.dto.PivotTreeReportResponse;
import com.marine.management.modules.finance.application.dto.TreeNodeDTO;
import com.marine.management.modules.finance.application.dto.TreeReportResponse;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.model.PivotReportProjection;
import com.marine.management.modules.finance.domain.model.TreeReportProjection;
import com.marine.management.modules.finance.domain.service.PivotReportBuilder;
import com.marine.management.modules.finance.domain.service.TreeReportBuilder;
import com.marine.management.modules.finance.infrastructure.FinancialEntryRepository;
import com.marine.management.modules.finance.infrastructure.FinancialEntryReportRepository;
import com.marine.management.modules.finance.presentation.dto.reports.DashboardSummary;
import com.marine.management.modules.finance.presentation.dto.reports.PivotReportRequest;
import com.marine.management.modules.finance.presentation.dto.reports.TreeReportRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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

    public DashboardSummary getDashboardSummary(LocalDate startDate, LocalDate endDate) {
        BigDecimal totalIncome = getTotalByType(RecordType.INCOME, startDate, endDate);
        BigDecimal totalExpense = getTotalByType(RecordType.EXPENSE, startDate, endDate);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        long incomeCount = countByType(RecordType.INCOME, startDate, endDate);
        long expenseCount = countByType(RecordType.EXPENSE, startDate, endDate);

        return new DashboardSummary(totalIncome, totalExpense, balance, incomeCount, expenseCount);
    }

    public List<FinancialEntryReportRepository.PeriodTotalProjection> getPeriodTotals(
            LocalDate startDate, LocalDate endDate) {
        return reportRepository.findPeriodTotals(startDate, endDate);
    }

    public List<FinancialEntryReportRepository.CategoryTotalProjection> getCategoryTotals(
            RecordType entryType, LocalDate startDate, LocalDate endDate) {
        return reportRepository.findCategoryTotals(entryType, startDate, endDate);
    }

    public List<FinancialEntryReportRepository.CategoryTotalProjection> getExpenseTotals(
            LocalDate startDate, LocalDate endDate) {
        return reportRepository.findExpenseTotals(startDate, endDate);
    }

    public List<FinancialEntryReportRepository.CategoryTotalProjection> getIncomeTotals(
            LocalDate startDate, LocalDate endDate) {
        return reportRepository.findIncomeTotals(startDate, endDate);
    }

    public List<FinancialEntryReportRepository.MonthlyTotalProjection> getMonthlyTotals(
            LocalDate startDate, LocalDate endDate) {
        return reportRepository.findMonthlyTotals(startDate, endDate);
    }

    public BigDecimal getTotalByType(RecordType entryType, LocalDate startDate, LocalDate endDate) {
        return reportRepository.sumByEntryTypeAndDateRange(entryType, startDate, endDate);
    }

    public long countByType(RecordType entryType, LocalDate startDate, LocalDate endDate) {
        return entryRepository.countByEntryTypeAndEntryDateBetween(entryType, startDate, endDate);
    }

    public List<FinancialEntryReportRepository.CategoryMonthBreakdownProjection> getCategoryMonthBreakdown(
            RecordType entryType, int year) {
        return reportRepository.findCategoryMonthBreakdown(entryType, year);
    }

    public List<FinancialEntryReportRepository.MonthlyIncomeExpenseProjection> getMonthlyIncomeExpense(int year) {
        return reportRepository.findMonthlyIncomeExpense(year);
    }



    public PivotTreeReportResponse generatePivotReport(PivotReportRequest request) {
        int year = request.year();

        List<PivotReportProjection> projections = reportRepository.findPivotProjections(
                RecordType.EXPENSE,
                year
        );

        return pivotBuilder.buildPivotReport(year, request.currency(), projections);
    }
}