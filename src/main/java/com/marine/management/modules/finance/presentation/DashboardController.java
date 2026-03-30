package com.marine.management.modules.finance.presentation;

import com.marine.management.modules.finance.application.FinancialReportService;
import com.marine.management.modules.finance.application.dto.CumulativeBalanceDTO;
import com.marine.management.modules.finance.application.usecase.GetCumulativeBalanceUseCase;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.vo.Period;
import com.marine.management.modules.finance.infrastructure.FinancialEntryReportRepository;
import com.marine.management.modules.finance.presentation.dto.reports.DashboardSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller for Dashboard endpoints.
 *
 * Handles dashboard KPIs and cumulative balance queries.
 */
@RestController
@RequestMapping("/api/finance/dashboard")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final GetCumulativeBalanceUseCase getCumulativeBalanceUseCase;
    private final FinancialReportService financialReportService;

    public DashboardController(
            GetCumulativeBalanceUseCase getCumulativeBalanceUseCase,
            FinancialReportService financialReportService
    ) {
        this.getCumulativeBalanceUseCase = getCumulativeBalanceUseCase;
        this.financialReportService = financialReportService;
    }

    // ============================================
    // CUMULATIVE BALANCE
    // ============================================

    @GetMapping("/cumulative-balance")
    public ResponseEntity<List<CumulativeBalanceDTO>> getCumulativeBalance(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        // Default to last year if not provided
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusYears(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        //  Period VO validation yapıyor!
        Period period = Period.of(start, end);

        log.info("GET /api/finance/dashboard/cumulative-balance - Period: {}", period.format());

        List<CumulativeBalanceDTO> result = getCumulativeBalanceUseCase.execute(period);

        log.info("Returning {} cumulative balance records", result.size());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/cumulative-balance/complete")
    public ResponseEntity<List<CumulativeBalanceDTO>> getCumulativeBalanceComplete(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        //  Period validation - tek satır!
        Period period = Period.of(startDate, endDate);

        log.info("GET /api/finance/dashboard/cumulative-balance/complete - Period: {}",
                period.format());

        List<CumulativeBalanceDTO> result = getCumulativeBalanceUseCase.executeWithCompletePeriod(period);

        log.info("Returning {} complete cumulative balance records", result.size());

        return ResponseEntity.ok(result);
    }

    // ============================================
    // DASHBOARD KPIs
    // ============================================

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummary> getDashboardSummary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().withDayOfYear(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        Period period = Period.of(start, end); //  Validation

        log.info("GET /api/finance/dashboard/summary - Period: {}", period.formatDetailed());

        DashboardSummary summary = financialReportService.getDashboardSummary(period);

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/period-totals")
    public ResponseEntity<List<FinancialEntryReportRepository.PeriodTotalProjection>> getPeriodTotals(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Period period = Period.of(startDate, endDate); //  Validation

        return ResponseEntity.ok(
                financialReportService.getPeriodTotals(period)
        );
    }

    @GetMapping("/category-totals")
    public ResponseEntity<List<FinancialEntryReportRepository.CategoryTotalProjection>> getCategoryTotals(
            @RequestParam RecordType entryType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Period period = Period.of(startDate, endDate); //  Validation

        return ResponseEntity.ok(financialReportService.getCategoryTotals(entryType, period));
    }

    @GetMapping("/expense-totals")
    public ResponseEntity<List<FinancialEntryReportRepository.CategoryTotalProjection>> getExpenseTotals(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Period period = Period.of(startDate, endDate); //  Validation

        return ResponseEntity.ok(financialReportService.getExpenseTotals(period));
    }

    @GetMapping("/income-totals")
    public ResponseEntity<List<FinancialEntryReportRepository.CategoryTotalProjection>> getIncomeTotals(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Period period = Period.of(startDate, endDate); //  Validation

        return ResponseEntity.ok(financialReportService.getIncomeTotals(period));
    }

    @GetMapping("/monthly-totals")
    public ResponseEntity<List<FinancialEntryReportRepository.MonthlyTotalProjection>> getMonthlyTotals(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Period period = Period.of(startDate, endDate); //  Validation

        return ResponseEntity.ok(financialReportService.getMonthlyTotals(period));
    }
}