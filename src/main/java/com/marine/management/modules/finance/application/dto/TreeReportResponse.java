package com.marine.management.modules.finance.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record TreeReportResponse(
        String period,
        String currency,
        BigDecimal totalAmount,
        List<TreeNodeDTO> nodes
) { }