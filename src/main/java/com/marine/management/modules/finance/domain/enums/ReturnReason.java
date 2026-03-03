package com.marine.management.modules.finance.domain.enums;

public enum ReturnReason {
    MISSING_RECEIPT("Missing receipt/invoice"),
    WRONG_AMOUNT("Wrong amount"),
    INCOMPLETE_DESCRIPTION("Incomplete description"),
    DUPLICATE_ENTRY("Duplicate entry"),
    OTHER("Other");

    private final String displayText;

    ReturnReason(String displayText) {
        this.displayText = displayText;
    }

    public String getDisplayText() {
        return displayText;
    }
}
