package com.marine.management.modules.finance.presentation.dto.controller;

import com.marine.management.modules.finance.domain.enums.RecordType;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record TextSearchRequest(
        @NotBlank(message = "Search term is required")
        String searchTerm,
        RecordType entryType,
        LocalDate startDate,
        LocalDate endDate,
        String sortColumn,
        String sortDirection,
        Integer page,
        Integer size
) {}