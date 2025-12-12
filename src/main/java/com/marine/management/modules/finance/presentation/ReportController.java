package com.marine.management.modules.finance.presentation;


import com.marine.management.modules.finance.application.FinancialReportService;
import com.marine.management.modules.finance.application.usecase.GenerateAnnualReportUseCase;
import com.marine.management.modules.finance.application.usecase.GeneratePeriodReportUseCase;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.infrastructure.FinancialEntryRepository;
import com.marine.management.modules.finance.presentation.dto.reports.AnnualBreakdownDto;
import com.marine.management.modules.finance.presentation.dto.reports.DashboardSummary;
import com.marine.management.modules.finance.presentation.dto.reports.PeriodBreakdownDto;
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
    private final FinancialReportService financialReportService;

    public ReportController(GenerateAnnualReportUseCase generateAnnualReportUseCase,
                            GeneratePeriodReportUseCase generatePeriodReportUseCase,
                            FinancialReportService financialReportService) {
        this.generateAnnualReportUseCase = generateAnnualReportUseCase;
        this.generatePeriodReportUseCase = generatePeriodReportUseCase;
        this.financialReportService = financialReportService;
    }

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
    // DASHBOARD & STATISTICS
    // ============================================

    @GetMapping("/dashboard/summary")
    public ResponseEntity<DashboardSummary> getDashboardSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        DashboardSummary summary =
                financialReportService.getDashboardSummary(start, end);

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/dashboard/period-totals")
    public ResponseEntity<List<FinancialEntryRepository.PeriodTotalProjection>> getPeriodTotals(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(financialReportService.getPeriodTotals(startDate, endDate));
    }

    @GetMapping("/dashboard/category-totals")
    public ResponseEntity<List<FinancialEntryRepository.CategoryTotalProjection>> getCategoryTotals(
            @RequestParam RecordType entryType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(
                financialReportService.getCategoryTotals(entryType, startDate, endDate)
        );
    }

    @GetMapping("/dashboard/expense-totals")
    public ResponseEntity<List<FinancialEntryRepository.CategoryTotalProjection>> getExpenseTotals(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(financialReportService.getExpenseTotals(startDate, endDate));
    }

    @GetMapping("/dashboard/income-totals")
    public ResponseEntity<List<FinancialEntryRepository.CategoryTotalProjection>> getIncomeTotals(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(financialReportService.getIncomeTotals(startDate, endDate));
    }

    @GetMapping("/dashboard/monthly-totals")
    public ResponseEntity<List<FinancialEntryRepository.MonthlyTotalProjection>> getMonthlyTotals(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(financialReportService.getMonthlyTotals(startDate, endDate));
    }


}