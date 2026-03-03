package com.marine.management.modules.finance.application.mapper;

import com.marine.management.modules.finance.application.dto.TreeNodeDTO;
import com.marine.management.modules.finance.application.dto.TreeReportResponse;
import com.marine.management.modules.finance.domain.model.TreeReportProjection;
import com.marine.management.modules.finance.domain.service.TreeReportBuilder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Maps Tree Report projections to hierarchical DTOs.
 */
@Component
public class TreeReportMapper {
    private final TreeReportBuilder treeBuilder;

    public TreeReportMapper(TreeReportBuilder treeBuilder) {
        this.treeBuilder = treeBuilder;
    }

    public TreeReportResponse toResponse(
            String period,
            String currency,
            List<TreeReportProjection> projections) {

        Objects.requireNonNull(period, "Period cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        Objects.requireNonNull(projections, "Projections cannot be null");

        List<TreeNodeDTO> nodes = treeBuilder.buildTree(projections);
        BigDecimal total = calculateTotalFromNodes(nodes);

        return new TreeReportResponse(period, currency, total, nodes);
    }

    private BigDecimal calculateTotalFromNodes(List<TreeNodeDTO> nodes) {
        return nodes.stream()
                .map(TreeNodeDTO::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}