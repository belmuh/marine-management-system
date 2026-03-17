package com.marine.management.modules.finance.presentation.dto.controller;

import com.marine.management.modules.finance.domain.enums.EntryStatus;
import com.marine.management.modules.finance.domain.enums.RecordType;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record EntrySearchRequest(
        UUID categoryId,
        RecordType entryType,
        Long whoId,
        Long mainCategoryId,
        Set<EntryStatus> status,
        LocalDate startDate,
        LocalDate endDate,
        String searchTerm,
        String sortColumn,
        String sortDirection,
        Integer page,
        Integer size
) {}
