package com.marine.management.modules.finance.domain;

import java.util.Arrays;

public enum EntryType {

    INCOME("Gelir"),
    EXPENSE("Gider");

    private final String displayName;

    EntryType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName(){
        return displayName;
    }

    public boolean isIncome() { return this == INCOME; }
    public boolean isExpense() { return this == EXPENSE; }

    public static EntryType fromDisplayName(String displayName) {
        return Arrays.stream(values())
                .filter(type -> type.displayName.equalsIgnoreCase(displayName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Geçersiz display name: " + displayName));
    }
}
