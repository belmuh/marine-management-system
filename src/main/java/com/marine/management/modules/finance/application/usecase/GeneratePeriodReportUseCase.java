package com.marine.management.modules.finance.application.usecase;

import com.marine.management.modules.finance.application.mapper.ReportMapper;
import com.marine.management.modules.finance.domain.model.Period;
import com.marine.management.modules.finance.domain.model.PeriodReport;
import com.marine.management.modules.finance.domain.service.ReportGenerator;
import com.marine.management.modules.finance.infrastructure.FinancialEntryRepository;
import com.marine.management.modules.finance.presentation.dto.reports.PeriodBreakdownDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class GeneratePeriodReportUseCase {

    private final FinancialEntryRepository repository;
    private final ReportGenerator reportGenerator;
    private final ReportMapper reportMapper;

    public GeneratePeriodReportUseCase(
            FinancialEntryRepository repository,
            ReportGenerator reportGenerator,
            ReportMapper reportMapper
    ) {
        this.repository = repository;
        this.reportGenerator = reportGenerator;
        this.reportMapper = reportMapper;
    }

    public PeriodBreakdownDto execute(LocalDate startDate, LocalDate endDate) {
        Period period = new Period(startDate, endDate);

        // Fetch data
        var entries = repository.findByEntryDateBetweenOrderByEntryDateDesc(
                period.startDate(),
                period.endDate()
        );

        // Generate report
        PeriodReport report = reportGenerator.generatePeriodReport(entries, period);

        // Map to DTO
        return reportMapper.toPeriodBreakdownDto(report);
    }
}