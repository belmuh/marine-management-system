package com.marine.management.modules.finance.domain;

import jakarta.persistence.Embeddable;

import java.time.Year;
import java.util.Objects;

@Embeddable
public class EntryNumber {

    private static final String PATTERN = "^FE-\\d{4}-\\d{3}$";

    private String value;

    protected EntryNumber() {};

    private EntryNumber(String value) {
        if (value == null || !value.matches(PATTERN)){
            throw new IllegalArgumentException(
                    "Entry number must match pattern: FE-YYYY-NNN"
            );
        }
        this.value = value;
    }

    public static EntryNumber of(String value) {
        return new EntryNumber(value);
    }

    public static  EntryNumber generate(int sequenceNumber) {
        int year = Year.now().getValue();
        String formatted = String.format("FE-%04d-%03d", year, sequenceNumber);
        return new EntryNumber(formatted);
    }

    public int getYear() {
        return Integer.parseInt(value.substring(3, 7));
    }

    public int getSequence() {
        return Integer.parseInt(value.substring(8));
    }

    public boolean isFromYear(int year) {
        return getYear() == year;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntryNumber)) return false;
        EntryNumber that = (EntryNumber) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
