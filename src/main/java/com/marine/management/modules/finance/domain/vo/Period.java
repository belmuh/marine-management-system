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

    public static Period ofMonth(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        return new Period(start, end);
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

    //  Yeni - Period'u formatla
    public String format() {
        if (isFullMonth()) {
            // "2026-01" format
            return String.format("%d-%02d",
                    startDate.getYear(),
                    startDate.getMonthValue()
            );
        } else if (isFullYear()) {
            // "2026" format
            return String.valueOf(startDate.getYear());
        } else {
            // "2026-01-15 to 2026-02-20" format
            return startDate + " to " + endDate;
        }
    }

    //  Yeni - Tam ay mı?
    public boolean isFullMonth() {
        return startDate.getDayOfMonth() == 1 &&
                endDate.equals(startDate.withDayOfMonth(startDate.lengthOfMonth()));
    }

    //  Yeni - Tam yıl mı?
    public boolean isFullYear() {
        return startDate.getDayOfYear() == 1 &&
                endDate.getDayOfYear() == endDate.lengthOfYear();
    }

    //  Yeni - Detaylı format (UI için)
    public String formatDetailed() {
        if (isFullMonth()) {
            return startDate.getMonth().name() + " " + startDate.getYear();
        } else if (isFullYear()) {
            return "Year " + startDate.getYear();
        } else {
            return startDate + " to " + endDate;
        }
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

    public static Period parse(String periodStr, String periodType) {
        if ("YEAR".equals(periodType)) {
            int year = Integer.parseInt(periodStr);
            return Period.ofYear(year);
        } else if ("MONTH".equals(periodType)) {
            String[] parts = periodStr.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            return Period.ofMonth(year, month);
        }
        throw new IllegalArgumentException("Invalid period format");
    }
}