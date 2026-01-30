package com.marine.management.modules.finance.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record TreeNodeDTO(
        Integer level,
        String type,
        String id,
        String name,
        String nameEn,
        BigDecimal amount,
        BigDecimal percentage,
        Boolean technical,
        Integer childCount,
        List<TreeNodeDTO> children
) { }
