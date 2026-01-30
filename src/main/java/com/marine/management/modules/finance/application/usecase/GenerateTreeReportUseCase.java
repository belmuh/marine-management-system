package com.marine.management.modules.finance.application.usecase;

import com.marine.management.modules.finance.application.dto.TreeNodeDTO;
import com.marine.management.modules.finance.application.dto.TreeReportResponse;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.model.TreeReportProjection;
import com.marine.management.modules.finance.domain.service.TreeReportBuilder;
import com.marine.management.modules.finance.domain.vo.Period;
import com.marine.management.modules.finance.infrastructure.FinancialEntryReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class GenerateTreeReportUseCase {

    private final FinancialEntryReportRepository reportRepository;
    private final TreeReportBuilder treeBuilder;

    public GenerateTreeReportUseCase(
            FinancialEntryReportRepository reportRepository,
            TreeReportBuilder treeBuilder
    ) {
        this.reportRepository = reportRepository;
        this.treeBuilder = treeBuilder;
    }

    public TreeReportResponse execute(
            Period period,           //  Domain VO burada
            RecordType recordType,
            String currency
    ) {
        // Query
        List<TreeReportProjection> projections = reportRepository.findTreeProjections(
                recordType,
                period.startDate(),
                period.endDate()
        );

        // Build tree
        List<TreeNodeDTO> nodes = treeBuilder.buildTree(projections);

        // Calculate total
        BigDecimal total = nodes.stream()
                .map(TreeNodeDTO::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TreeReportResponse(
                period.format(),     //  Domain logic
                currency,
                total,
                nodes
        );
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        if (startDate.isBefore(LocalDate.now().minusYears(10))) {
            throw new IllegalArgumentException("Start date too far in the past");
        }
    }
}