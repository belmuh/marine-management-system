package com.marine.management.modules.finance.application.usecase;

import com.marine.management.modules.finance.application.dto.PivotTreeReportResponse;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.model.PivotReportProjection;
import com.marine.management.modules.finance.domain.service.PivotReportBuilder;
import com.marine.management.modules.finance.infrastructure.FinancialEntryReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Use Case: Generate Pivot Tree Report
 *
 * Produces hierarchical pivot view of financial data grouped by category,
 * subcategory, and who, with monthly breakdown.
 *
 * <p>Flow:
 * <ol>
 *   <li>Fetch pivot projections from database (Infrastructure)</li>
 *   <li>Build pivot tree structure (Domain)</li>
 * </ol>
 */
@Service
@Transactional(readOnly = true)
public class GeneratePivotTreeUseCase {

    private static final int MIN_YEAR = 2000;

    private final FinancialEntryReportRepository reportRepository;
    private final PivotReportBuilder pivotBuilder;

    public GeneratePivotTreeUseCase(
            FinancialEntryReportRepository reportRepository,
            PivotReportBuilder pivotBuilder
    ) {
        this.reportRepository = Objects.requireNonNull(reportRepository);
        this.pivotBuilder = Objects.requireNonNull(pivotBuilder);
    }

    /**
     * Generates pivot tree report for the given year and entry type.
     *
     * @param year Year to generate report for (2000 to current year)
     * @param entryType INCOME or EXPENSE (not null)
     * @param currency Currency code, e.g., EUR, USD (not null)
     * @return Pivot tree report with hierarchical monthly breakdown
     * @throws IllegalArgumentException if year is out of range
     * @throws NullPointerException if entryType or currency is null
     */
    public PivotTreeReportResponse execute(int year, RecordType entryType, String currency) {
        validateYear(year);
        Objects.requireNonNull(entryType, "Entry type cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");

        // Fetch pivot projections
        List<PivotReportProjection> projections = reportRepository.findPivotProjections(
                entryType,
                year
        );

        // Build pivot tree structure
        return pivotBuilder.buildPivotReport(year, currency, projections);
    }

    /**
     * Validates year is within acceptable range.
     *
     * @throws IllegalArgumentException if year is out of range
     */
    private void validateYear(int year) {
        int currentYear = java.time.Year.now().getValue();
        if (year < MIN_YEAR || year > currentYear) {
            throw new IllegalArgumentException(
                    String.format("Year must be between %d and %d, got: %d",
                            MIN_YEAR, currentYear, year)
            );
        }
    }
}