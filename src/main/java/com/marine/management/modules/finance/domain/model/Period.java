package com.marine.management.modules.finance.domain.valueObjects;

import java.time.LocalDate;

public record Period(LocalDate start, LocalDate end) {
    public Period {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start must be before end");
        }
    }

    public int getYear() {
        return start.getYear();
    }

    public boolean isFullYear() {
        return start.getMonthValue() == 1 && end.getMonthValue() == 12;
    }
}