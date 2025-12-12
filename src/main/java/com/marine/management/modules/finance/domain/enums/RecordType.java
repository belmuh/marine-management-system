package com.marine.management.modules.finance.domain.enums;

import java.util.Arrays;

public enum RecordType {

    INCOME("Gelir"),
    EXPENSE("Gider");

    private final String displayName;

    RecordType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName(){
        return displayName;
    }

    public boolean isIncome() { return this == INCOME; }
    public boolean isExpense() { return this == EXPENSE; }

    public static RecordType fromDisplayName(String displayName) {
        return Arrays.stream(values())
                .filter(type -> type.displayName.equalsIgnoreCase(displayName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Geçersiz display name: " + displayName));
    }
}
