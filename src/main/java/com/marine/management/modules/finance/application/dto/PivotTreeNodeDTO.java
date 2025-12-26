package com.marine.management.modules.finance.application.dto;



import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PivotTreeNodeDTO(
        String id,
        Integer level,
        String type,
        String name,
        String nameEn,
        Boolean isTechnical,
        Map<String, BigDecimal> monthlyValues,
        List<PivotTreeNodeDTO> children
) { }

