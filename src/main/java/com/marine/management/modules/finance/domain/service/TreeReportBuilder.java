package com.marine.management.modules.finance.domain.service;

import com.marine.management.modules.finance.application.dto.TreeNodeDTO;
import com.marine.management.modules.finance.domain.enums.NodeType;
import com.marine.management.modules.finance.domain.model.TreeReportProjection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static com.marine.management.modules.finance.domain.service.TreeReportConstants.*;

/**
 * Builds hierarchical tree structure from flat database projections.
 *
 * <p>Tree hierarchy:
 * <pre>
 * Main Category
 *   └─ Category
 *       └─ Who
 * </pre>
 */
@Component
public class TreeReportBuilder {

    public List<TreeNodeDTO> buildTree(List<TreeReportProjection> projections) {
        Objects.requireNonNull(projections, "Projections cannot be null - this indicates a bug");

        if (projections.isEmpty()) {
            return Collections.emptyList(); // Empty result is valid (no data for period)
        }

        BigDecimal grandTotal = calculateGrandTotal(projections);

        Map<Long, List<TreeReportProjection>> mainCategoryGroups = groupByMainCategory(projections);

        List<TreeNodeDTO> mainCategoryNodes = mainCategoryGroups.entrySet().stream()
                .map(entry -> buildMainCategoryNode(entry.getKey(), entry.getValue(), grandTotal))
                .sorted(Comparator.comparing(TreeNodeDTO::amount).reversed())
                .collect(Collectors.toList());

        return mainCategoryNodes;
    }

    // ========== Main Category Level ==========

    private TreeNodeDTO buildMainCategoryNode(
            Long mainCatId,
            List<TreeReportProjection> projections,
            BigDecimal grandTotal) {

        BigDecimal mainCatTotal = calculateGroupTotal(projections);
        List<TreeNodeDTO> categoryChildren = buildCategoryLevel(projections, mainCatTotal);

        if (mainCatId.equals(UNASSIGNED_ID)) {
            return TreeNodeDTO.unassignedMainCategory(
                    mainCatTotal,
                    calculatePercentage(mainCatTotal, grandTotal),
                    categoryChildren
            );
        }

        var first = projections.get(0);
        return new TreeNodeDTO(
                NodeType.MAIN_CATEGORY.getLevel(),
                NodeType.MAIN_CATEGORY.getTypeName(),
                mainCatId.toString(),
                first.mainCategoryNameTr(),
                first.mainCategoryNameEn(),
                mainCatTotal,
                calculatePercentage(mainCatTotal, grandTotal),
                first.mainCategoryTechnical(),
                categoryChildren.size(),
                categoryChildren
        );
    }

    // ========== Category Level ==========

    private List<TreeNodeDTO> buildCategoryLevel(
            List<TreeReportProjection> projections,
            BigDecimal parentTotal) {

        Map<UUID, List<TreeReportProjection>> categoryGroups = projections.stream()
                .collect(Collectors.groupingBy(TreeReportProjection::categoryId));

        return categoryGroups.entrySet().stream()
                .map(entry -> buildCategoryNode(entry.getKey(), entry.getValue(), parentTotal))
                .sorted(Comparator.comparing(TreeNodeDTO::amount).reversed())
                .collect(Collectors.toList());
    }

    private TreeNodeDTO buildCategoryNode(
            UUID categoryId,
            List<TreeReportProjection> projections,
            BigDecimal parentTotal) {

        BigDecimal categoryTotal = calculateGroupTotal(projections);
        List<TreeNodeDTO> whoChildren = buildWhoLevel(projections, categoryTotal);

        var first = projections.get(0);
        return new TreeNodeDTO(
                NodeType.CATEGORY.getLevel(),
                NodeType.CATEGORY.getTypeName(),
                categoryId.toString(),
                first.categoryName(),
                first.categoryName(),
                categoryTotal,
                calculatePercentage(categoryTotal, parentTotal),
                first.categoryTechnical(),
                whoChildren.size(),
                whoChildren
        );
    }

    // ========== Who Level ==========

    private List<TreeNodeDTO> buildWhoLevel(
            List<TreeReportProjection> projections,
            BigDecimal parentTotal) {

        Map<Long, List<TreeReportProjection>> whoGroups = groupByWho(projections);

        return whoGroups.entrySet().stream()
                .map(entry -> buildWhoNode(entry.getKey(), entry.getValue(), parentTotal))
                .sorted(Comparator.comparing(TreeNodeDTO::amount).reversed())
                .collect(Collectors.toList());
    }

    private TreeNodeDTO buildWhoNode(
            Long whoId,
            List<TreeReportProjection> projections,
            BigDecimal parentTotal) {

        BigDecimal whoTotal = calculateGroupTotal(projections);

        if (whoId.equals(UNASSIGNED_ID)) {
            return TreeNodeDTO.unspecifiedWho(
                    whoTotal,
                    calculatePercentage(whoTotal, parentTotal)
            );
        }

        var first = projections.get(0);
        return new TreeNodeDTO(
                NodeType.WHO.getLevel(),
                NodeType.WHO.getTypeName(),
                whoId.toString(),
                first.whoNameTr(),
                first.whoNameEn(),
                whoTotal,
                calculatePercentage(whoTotal, parentTotal),
                first.whoTechnical(),
                0,
                Collections.emptyList()
        );
    }

    // ========== Helper Methods ==========

    private Map<Long, List<TreeReportProjection>> groupByMainCategory(
            List<TreeReportProjection> projections) {
        return projections.stream()
                .collect(Collectors.groupingBy(
                        p -> p.mainCategoryId() != null ? p.mainCategoryId() : UNASSIGNED_ID
                ));
    }

    private Map<Long, List<TreeReportProjection>> groupByWho(
            List<TreeReportProjection> projections) {
        return projections.stream()
                .collect(Collectors.groupingBy(
                        p -> p.whoId() != null ? p.whoId() : UNASSIGNED_ID
                ));
    }

    private BigDecimal calculateGrandTotal(List<TreeReportProjection> projections) {
        return projections.stream()
                .map(TreeReportProjection::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateGroupTotal(List<TreeReportProjection> projections) {
        return projections.stream()
                .map(TreeReportProjection::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculatePercentage(BigDecimal amount, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return amount
                .divide(total, PERCENTAGE_DIVISION_SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(PERCENTAGE_SCALE, RoundingMode.HALF_UP);
    }
}