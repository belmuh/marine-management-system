package com.marine.management.modules.finance.application.usecase;

import com.marine.management.modules.finance.application.mapper.ReportMapper;
import com.marine.management.modules.finance.domain.model.AnnualReport;
import com.marine.management.modules.finance.domain.vo.Period;
import com.marine.management.modules.finance.domain.service.ReportGenerator;
import com.marine.management.modules.finance.infrastructure.FinancialEntryRepository;
import com.marine.management.modules.finance.presentation.dto.reports.AnnualBreakdownDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GenerateAnnualReportUseCase {

    private final FinancialEntryRepository repository;
    private final ReportGenerator reportGenerator;
    private final ReportMapper reportMapper;

    public GenerateAnnualReportUseCase(
            FinancialEntryRepository repository,
            ReportGenerator reportGenerator,
            ReportMapper reportMapper
    ) {
        this.repository = repository;
        this.reportGenerator = reportGenerator;
        this.reportMapper = reportMapper;
    }

    public AnnualBreakdownDto execute(int year) {
        validateYear(year);

        // 1. Fetch data from repository
        Period period = Period.ofYear(year);
        var entries = repository.findByEntryDateBetweenOrderByEntryDateDesc(
                period.startDate(),
                period.endDate()
        );

        // 2. Generate report using domain service
        AnnualReport report = reportGenerator.generateAnnualReport(entries, year);

        // 3. Map to DTO
        return reportMapper.toAnnualBreakdownDto(report);
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