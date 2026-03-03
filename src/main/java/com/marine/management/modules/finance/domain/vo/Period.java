package com.marine.management.modules.finance.domain.vo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

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

    public static Period ofQuarter(int year, int quarter) { // ✨ YENİ
        if (quarter < 1 || quarter > 4) {
            throw new IllegalArgumentException("Quarter must be between 1 and 4");
        }
        int startMonth = (quarter - 1) * 3 + 1;
        int endMonth = startMonth + 2;
        return new Period(
                LocalDate.of(year, startMonth, 1),
                LocalDate.of(year, endMonth, 1).withDayOfMonth(
                        LocalDate.of(year, endMonth, 1).lengthOfMonth()
                )
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

    public long getMonthsCount() {
        return ChronoUnit.MONTHS.between(startDate, endDate) + 1;
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public boolean overlaps(Period other) {
        return !this.endDate.isBefore(other.startDate) &&
                !other.endDate.isBefore(this.startDate);
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

    public boolean isFullQuarter() { // ✨ YENİ
        int startMonth = startDate.getMonthValue();
        int endMonth = endDate.getMonthValue();

        return startDate.getDayOfMonth() == 1 &&
                endDate.equals(endDate.withDayOfMonth(endDate.lengthOfMonth())) &&
                (startMonth - 1) % 3 == 0 && // Q1:1, Q2:4, Q3:7, Q4:10
                endMonth - startMonth == 2;
    }

    /**
     * Parses period string with type hint.
     *
     * @param periodStr Period string (e.g., "2026" or "2026-01")
     * @param periodType Type hint ("YEAR" or "MONTH")
     * @return Parsed Period
     */
    public static Period parse(String periodStr, String periodType) {
        Objects.requireNonNull(periodStr, "Period string cannot be null");
        Objects.requireNonNull(periodType, "Period type cannot be null");

        return switch (periodType.toUpperCase()) { // ✅ Switch expression (Java 17)
            case "YEAR" -> {
                int year = Integer.parseInt(periodStr);
                yield Period.ofYear(year);
            }
            case "MONTH" -> {
                String[] parts = periodStr.split("-");
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Month format must be YYYY-MM");
                }
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                yield Period.ofMonth(year, month);
            }
            case "QUARTER" -> { // ✨ YENİ
                String[] parts = periodStr.split("-Q");
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Quarter format must be YYYY-Q#");
                }
                int year = Integer.parseInt(parts[0]);
                int quarter = Integer.parseInt(parts[1]);
                yield Period.ofQuarter(year, quarter);
            }
            default -> throw new IllegalArgumentException(
                    "Invalid period type: " + periodType + ". Expected: YEAR, MONTH, or QUARTER"
            );
        };
    }
    /**
     * Formats period in compact form.
     *
     * Examples:
     * - Full month: "2026-01"
     * - Full year: "2026"
     * - Custom range: "2026-01-15 to 2026-02-20"
     */
    public String format() {
        if (isFullYear()) {
            return String.valueOf(startDate.getYear());
        } else if (isFullMonth()) {
            return String.format("%d-%02d",
                    startDate.getYear(),
                    startDate.getMonthValue()
            );
        } else if (isFullQuarter()) { // ✨ YENİ
            int quarter = (startDate.getMonthValue() - 1) / 3 + 1;
            return String.format("%d-Q%d", startDate.getYear(), quarter);
        } else {
            return startDate + " to " + endDate;
        }
    }

    /**
     * Formats period in human-readable form.
     *
     * Examples:
     * - Full month: "January 2026"
     * - Full year: "Year 2026"
     * - Full quarter: "Q1 2026"
     * - Custom range: "2026-01-15 to 2026-02-20"
     */
    public String formatDetailed() {
        if (isFullYear()) {
            return "Year " + startDate.getYear();
        } else if (isFullMonth()) {
            return startDate.getMonth().name() + " " + startDate.getYear();
        } else if (isFullQuarter()) { // ✨ YENİ
            int quarter = (startDate.getMonthValue() - 1) / 3 + 1;
            return "Q" + quarter + " " + startDate.getYear();
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

        // ✅ Business rule: 10 yıl öncesi limit
        LocalDate tenYearsAgo = LocalDate.now().minusYears(10);
        if (start.isBefore(tenYearsAgo)) {
            throw new IllegalArgumentException(
                    String.format(
                            "Start date cannot be more than 10 years in the past (before %s)",
                            tenYearsAgo
                    )
            );
        }

        // ✅ Business rule: Gelecekte 1 yıl limit
        LocalDate oneYearAhead = LocalDate.now().plusYears(1);
        if (end.isAfter(oneYearAhead)) {
            throw new IllegalArgumentException(
                    String.format(
                            "End date cannot be more than 1 year in the future (after %s)",
                            oneYearAhead
                    )
            );
        }
    }


}