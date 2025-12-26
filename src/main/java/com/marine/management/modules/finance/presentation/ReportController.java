package com.marine.management.modules.finance.presentation;


import com.marine.management.modules.finance.application.FinancialReportService;
import com.marine.management.modules.finance.application.dto.*;
import com.marine.management.modules.finance.application.usecase.GenerateAnnualReportUseCase;
import com.marine.management.modules.finance.application.usecase.GeneratePeriodReportUseCase;
import com.marine.management.modules.finance.domain.entity.FinancialEntry;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.model.ExpenseTreeReport;
import com.marine.management.modules.finance.domain.model.PivotTreeNode;
import com.marine.management.modules.finance.domain.model.PivotTreeReport;
import com.marine.management.modules.finance.domain.model.TreeNode;
import com.marine.management.modules.finance.domain.service.PivotTreeReportGenerator;
import com.marine.management.modules.finance.domain.service.TreeReportGenerator;
import com.marine.management.modules.finance.infrastructure.FinancialEntryRepository;
import com.marine.management.modules.finance.presentation.dto.reports.AnnualBreakdownDto;
import com.marine.management.modules.finance.presentation.dto.reports.DashboardSummary;
import com.marine.management.modules.finance.presentation.dto.reports.PeriodBreakdownDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import com.marine.management.modules.finance.domain.vo.Period;
import java.util.List;

@RestController
@RequestMapping("/api/finance/reports")
public class ReportController {

    private final GenerateAnnualReportUseCase generateAnnualReportUseCase;
    private final GeneratePeriodReportUseCase generatePeriodReportUseCase;
    private final FinancialReportService financialReportService;
    private final TreeReportGenerator treeReportGenerator;
    private final FinancialEntryRepository entryRepository;
    private final PivotTreeReportGenerator pivotTreeReportGenerator;

    public ReportController(GenerateAnnualReportUseCase generateAnnualReportUseCase,
                            GeneratePeriodReportUseCase generatePeriodReportUseCase,
                            FinancialReportService financialReportService,
                            TreeReportGenerator treeReportGenerator,
                            FinancialEntryRepository entryRepository,
                            PivotTreeReportGenerator pivotTreeReportGenerator) {
        this.generateAnnualReportUseCase = generateAnnualReportUseCase;
        this.generatePeriodReportUseCase = generatePeriodReportUseCase;
        this.financialReportService = financialReportService;
        this.treeReportGenerator = treeReportGenerator;
        this.entryRepository = entryRepository;
        this.pivotTreeReportGenerator = pivotTreeReportGenerator;
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

    public record TreeReportRequest(
            String startDate,    // "2025-01-01"
            String endDate,      // "2025-01-31"
            String currency      // "EUR"
    ) { }

    @PostMapping("/expense-tree")
    public TreeReportResponse getExpenseTree(
            @RequestBody TreeReportRequest request
    ) {
        LocalDate start = LocalDate.parse(request.startDate());
        LocalDate end = LocalDate.parse(request.endDate());
        Period period = Period.of(start, end);

        // Get all entries
        List<FinancialEntry> entries = entryRepository.findAll();

        // Generate report
        ExpenseTreeReport report = treeReportGenerator.generateExpenseTree(
                entries,
                period,
                request.currency()
        );

        // Map to DTO
        return mapToResponse(report);
    }

    private TreeReportResponse mapToResponse(ExpenseTreeReport report) {
        List<TreeNodeDTO> nodeDTOs = report.getNodes().stream()
                .map(this::mapTreeNode)
                .toList();

        return new TreeReportResponse(
                report.getPeriod().toString(), // veya formatla
                report.getCurrency(),
                report.getTotalAmount(),
                nodeDTOs
        );
    }

    private TreeNodeDTO mapTreeNode(TreeNode node) {
        List<TreeNodeDTO> childrenDTOs = null;
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            childrenDTOs = node.getChildren().stream()
                    .map(this::mapTreeNode)
                    .toList();
        }

        return new TreeNodeDTO(
                node.getLevel(),
                node.getType().name(),
                node.getId(),
                node.getName(),
                node.getNameEn(),
                node.getAmount(),
                node.getPercentage(),
                node.getIsTechnical(),
                node.getChildCount(),
                childrenDTOs
        );
    }

    @GetMapping("/expense-tree-pivot")
    public PivotTreeReportResponse getExpenseTreePivot(
            @RequestParam int year,
            @RequestParam(defaultValue = "EUR") String currency
    ) {
        List<FinancialEntry> entries = entryRepository.findAll();

        PivotTreeReport report = pivotTreeReportGenerator.generatePivotReport(
                entries,
                year,
                currency
        );

        return mapToPivotResponse(report);
    }

    private PivotTreeReportResponse mapToPivotResponse(PivotTreeReport report) {
        List<PivotTreeNodeDTO> rows = report.getRows().stream()
                .map(this::mapPivotNode)
                .toList();

        return new PivotTreeReportResponse(
                report.getYear(),
                report.getCurrency(),
                report.getColumns(),
                report.getColumnTotals(),
                rows
        );
    }

    private PivotTreeNodeDTO mapPivotNode(PivotTreeNode node) {
        List<PivotTreeNodeDTO> children = node.getChildren() != null
                ? node.getChildren().stream().map(this::mapPivotNode).toList()
                : List.of();

        return new PivotTreeNodeDTO(
                node.getId(),
                node.getLevel(),
                node.getType(),
                node.getName(),
                node.getNameEn(),
                node.getIsTechnical(),
                node.getMonthlyValues(),
                children
        );
    }

}