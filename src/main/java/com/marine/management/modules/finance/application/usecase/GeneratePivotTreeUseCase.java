package com.marine.management.modules.finance.application.usecase;

import com.marine.management.modules.finance.application.dto.PivotTreeReportResponse;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.model.PivotReportProjection;
import com.marine.management.modules.finance.domain.service.PivotReportBuilder;
import com.marine.management.modules.finance.infrastructure.FinancialEntryReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GeneratePivotTreeUseCase {

    private final FinancialEntryReportRepository reportRepository;
    private final PivotReportBuilder pivotBuilder;

    public GeneratePivotTreeUseCase(
            FinancialEntryReportRepository reportRepository,
            PivotReportBuilder pivotBuilder
    ) {
        this.reportRepository = reportRepository;
        this.pivotBuilder = pivotBuilder;
    }

    public PivotTreeReportResponse execute(int year, RecordType entryType, String currency) {
        validateYear(year);

        // 1. Fetch projections from repository
        List<PivotReportProjection> projections = reportRepository.findPivotProjections(
                entryType,
                year
        );

        // 2. Build pivot report using domain service
        return pivotBuilder.buildPivotReport(year, currency, projections);
    }

    private void validateYear(int year) {
        int currentYear = java.time.Year.now().getValue();
        if (year < 2000 || year > currentYear) {
            throw new IllegalArgumentException(
                    "Year must be between 2000 and " + currentYear
            );
        }
    }
}