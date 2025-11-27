package com.marine.management.modules.finance.presentation;


import com.marine.management.modules.finance.application.usecase.GenerateAnnualReportUseCase;
import com.marine.management.modules.finance.application.usecase.GeneratePeriodReportUseCase;
import com.marine.management.modules.finance.presentation.dto.reports.AnnualBreakdownDto;
import com.marine.management.modules.finance.presentation.dto.reports.PeriodBreakdownDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/finance/reports")
public class ReportController {

    private final GenerateAnnualReportUseCase generateAnnualReportUseCase;
    private final GeneratePeriodReportUseCase generatePeriodReportUseCase;

    public ReportController(GenerateAnnualReportUseCase generateAnnualReportUseCase, GeneratePeriodReportUseCase generatePeriodReportUseCase) {
        this.generateAnnualReportUseCase = generateAnnualReportUseCase;
        this.generatePeriodReportUseCase = generatePeriodReportUseCase;
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
}