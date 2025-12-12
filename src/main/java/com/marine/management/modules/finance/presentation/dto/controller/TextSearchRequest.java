package com.marine.management.modules.finance.presentation.dto.controller;

import com.marine.management.modules.finance.domain.enums.RecordType;

import java.time.LocalDate;

public  record TextSearchRequest(
        String searchTerm,
        RecordType entryType,
        LocalDate startDate,
        LocalDate endDate
) {
}
