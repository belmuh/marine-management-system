package com.marine.management.modules.finance.presentation;

import com.marine.management.modules.finance.application.FinancialReportService;
import com.marine.management.modules.finance.application.dto.*;
import com.marine.management.modules.finance.application.usecase.GenerateAnnualReportUseCase;
import com.marine.management.modules.finance.application.usecase.GeneratePeriodReportUseCase;
import com.marine.management.modules.finance.application.usecase.GeneratePivotTreeUseCase;
import com.marine.management.modules.finance.application.usecase.GenerateTreeReportUseCase;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.vo.Period;
import com.marine.management.modules.finance.infrastructure.FinancialEntryReportRepository;
import com.marine.management.modules.finance.presentation.dto.reports.AnnualBreakdownDto;
import com.marine.management.modules.finance.presentation.dto.reports.DashboardSummary;
import com.marine.management.modules.finance.presentation.dto.reports.PeriodBreakdownDto;
import com.marine.management.modules.finance.presentation.dto.reports.TreeReportRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/finance/reports")
public class ReportController {

    private final GenerateAnnualReportUseCase generateAnnualReportUseCase;
    private final GeneratePeriodReportUseCase generatePeriodReportUseCase;
    private final GenerateTreeReportUseCase generateTreeReportUseCase;        // ✅ Yeni
    private final GeneratePivotTreeUseCase generatePivotTreeUseCase;          // ✅ Yeni
    private final FinancialReportService financialReportService;

    public ReportController(
            GenerateAnnualReportUseCase generateAnnualReportUseCase,
            GeneratePeriodReportUseCase generatePeriodReportUseCase,
            GenerateTreeReportUseCase generateTreeReportUseCase,              // ✅ Yeni
            GeneratePivotTreeUseCase generatePivotTreeUseCase,                // ✅ Yeni
            FinancialReportService financialReportService
    ) {
        this.generateAnnualReportUseCase = generateAnnualReportUseCase;
        this.generatePeriodReportUseCase = generatePeriodReportUseCase;
        this.generateTreeReportUseCase = generateTreeReportUseCase;          // ✅ Yeni
        this.generatePivotTreeUseCase = generatePivotTreeUseCase;            // ✅ Yeni
        this.financialReportService = financialReportService;
    }


    // ============================================
    // BREAKDOWN REPORTS
    // ============================================

    @GetMapping("/annual-breakdown/{year}")
    public ResponseEntity<AnnualBreakdownDto> getAnnualBreakdown(@PathVariable int year) {
        AnnualBreakdownDto breakdown = generateAnnualReportUseCase.execute(year);
        return ResponseEntity.ok(breakdown);
    }

    @GetMapping("/period-breakdown")
    public ResponseEntity<PeriodBreakdownDto> getPeriodBreakdown(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        PeriodBreakdownDto breakdown = generatePeriodReportUseCase.execute(startDate, endDate);
        return ResponseEntity.ok(breakdown);
    }

    // ============================================
    // DASHBOARD REPORTS
    // ============================================

    @GetMapping("/dashboard/summary")
    public ResponseEntity<DashboardSummary> getDashboardSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        DashboardSummary summary = financialReportService.getDashboardSummary(start, end);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/dashboard/period-totals")
    public ResponseEntity<List<FinancialEntryReportRepository.PeriodTotalProjection>> getPeriodTotals(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(financialReportService.getPeriodTotals(startDate, endDate));
    }

    @GetMapping("/dashboard/category-totals")
    public ResponseEntity<List<FinancialEntryReportRepository.CategoryTotalProjection>> getCategoryTotals(
            @RequestParam RecordType entryType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(financialReportService.getCategoryTotals(entryType, startDate, endDate));
    }

    @GetMapping("/dashboard/expense-totals")
    public ResponseEntity<List<FinancialEntryReportRepository.CategoryTotalProjection>> getExpenseTotals(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(financialReportService.getExpenseTotals(startDate, endDate));
    }

    @GetMapping("/dashboard/income-totals")
    public ResponseEntity<List<FinancialEntryReportRepository.CategoryTotalProjection>> getIncomeTotals(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(financialReportService.getIncomeTotals(startDate, endDate));
    }

    @GetMapping("/dashboard/monthly-totals")
    public ResponseEntity<List<FinancialEntryReportRepository.MonthlyTotalProjection>> getMonthlyTotals(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(financialReportService.getMonthlyTotals(startDate, endDate));
    }

    // ============================================
    // TREE REPORTS
    // ============================================

    @PostMapping("/expense-tree")
    public ResponseEntity<TreeReportResponse> getExpenseTree(
            @RequestBody TreeReportRequest request
    ) {
        Period period = Period.of(request.startDate(), request.endDate());

        TreeReportResponse response = generateTreeReportUseCase.execute(
                period,
                RecordType.EXPENSE,
                request.currency()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/income-tree")
    public ResponseEntity<TreeReportResponse> getIncomeTree(
            @RequestBody TreeReportRequest request
    ) {
        Period period = Period.of(request.startDate(), request.endDate());

        TreeReportResponse response = generateTreeReportUseCase.execute(
                period,
                RecordType.INCOME,
                request.currency()
        );
        return ResponseEntity.ok(response);
    }

    // ============================================
    // PIVOT REPORTS
    // ============================================

    @GetMapping("/expense-tree-pivot")
    public ResponseEntity<PivotTreeReportResponse> getExpenseTreePivot(
            @RequestParam int year,
            @RequestParam(defaultValue = "EUR") String currency
    ) {
        PivotTreeReportResponse response = generatePivotTreeUseCase.execute(
                year,
                RecordType.EXPENSE,
                currency
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/income-tree-pivot")
    public ResponseEntity<PivotTreeReportResponse> getIncomeTreePivot(
            @RequestParam int year,
            @RequestParam(defaultValue = "EUR") String currency
    ) {
        PivotTreeReportResponse response = generatePivotTreeUseCase.execute(
                year,
                RecordType.INCOME,
                currency
        );
        return ResponseEntity.ok(response);
    }
}