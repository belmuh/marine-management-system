package com.marine.management.modules.finance.domain.vo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record Period(LocalDate startDate, LocalDate endDate) {

    public Period {
        validatePeriod(startDate, endDate);
    }

    public static Period of(LocalDate startDate, LocalDate endDate) {
        return new Period(startDate, endDate);
    }

    public static Period ofYear(int year) {
        return new Period(
                LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31)
        );
    }

    public static Period ofFirstHalf(int year) {
        return new Period(
                LocalDate.of(year, 1, 1),
                LocalDate.of(year, 6, 30)
        );
    }

    public static Period ofSecondHalf(int year) {
        return new Period(
                LocalDate.of(year, 7, 1),
                LocalDate.of(year, 12, 31)
        );
    }

    public int getYear() {
        return startDate.getYear();
    }

    public long getDaysCount() {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    private static void validatePeriod(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Period dates cannot be null");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException(
                    "Start date must be before or equal to end date"
            );
        }
    }
}