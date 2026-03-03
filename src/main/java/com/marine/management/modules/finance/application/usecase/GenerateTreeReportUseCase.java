package com.marine.management.modules.finance.application.usecase;

import com.marine.management.modules.finance.application.dto.TreeReportResponse;
import com.marine.management.modules.finance.application.mapper.TreeReportMapper;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.model.TreeReportProjection;
import com.marine.management.modules.finance.domain.vo.Period;
import com.marine.management.modules.finance.infrastructure.FinancialEntryReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Use Case: Generate Tree Report
 *
 * Produces hierarchical view of financial data grouped by main category,
 * category, and who (person/company).
 *
 * <p>Flow:
 * <ol>
 *   <li>Fetch flat projections from database (Infrastructure)</li>
 *   <li>Build hierarchical tree structure (Domain - via Mapper)</li>
 *   <li>Wrap in response DTO (Application)</li>
 * </ol>
 */
@Service
@Transactional(readOnly = true)
public class GenerateTreeReportUseCase {

    private final FinancialEntryReportRepository reportRepository;
    private final TreeReportMapper treeReportMapper;

    public GenerateTreeReportUseCase(
            FinancialEntryReportRepository reportRepository,
            TreeReportMapper treeReportMapper
    ) {
        this.reportRepository = reportRepository;
        this.treeReportMapper = treeReportMapper;
    }

    /**
     * Generates tree report for the given period and record type.
     *
     * @param period Time period for the report (not null)
     * @param recordType INCOME or EXPENSE (not null)
     * @param currency Currency code, e.g., EUR, USD (not null)
     * @return Tree report response with hierarchical structure
     * @throws NullPointerException if any parameter is null
     */
    public TreeReportResponse execute(
            Period period,
            RecordType recordType,
            String currency
    ) {
        // Validate inputs
        Objects.requireNonNull(period, "Period cannot be null");
        Objects.requireNonNull(recordType, "RecordType cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");

        // Fetch projections from database
        List<TreeReportProjection> projections = reportRepository.findTreeProjections(
                recordType,
                period.startDate(),
                period.endDate()
        );

        // Build tree and wrap in response (delegation to mapper)
        return treeReportMapper.toResponse(period.format(), currency, projections);
    }
}