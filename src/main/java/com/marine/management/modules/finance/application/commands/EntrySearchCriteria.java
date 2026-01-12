package com.marine.management.modules.finance.application.commands;

import com.marine.management.modules.finance.domain.enums.RecordType;

import java.time.LocalDate;
import java.util.UUID;

public record EntrySearchCriteria(
        UUID categoryId,
        RecordType entryType,
        Long whoId,
        Long mainCategoryId,
        LocalDate startDate,
        LocalDate endDate
) {}