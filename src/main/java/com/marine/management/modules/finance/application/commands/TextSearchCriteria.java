package com.marine.management.modules.finance.application.commands;


import com.marine.management.modules.finance.domain.enums.RecordType;

import java.time.LocalDate;

public record TextSearchCriteria(
        String searchTerm,
        RecordType entryType,
        LocalDate startDate,
        LocalDate endDate
) {}