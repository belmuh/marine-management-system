package com.marine.management.modules.finance.domain.enums;


public enum PaymentMethod {
    CASH("Nakit"),
    BANK_TRANSFER("Banka Havalesi"),
    CREDIT_CARD("Kredi Kartı"),
    DEBIT_CARD("Banka Kartı");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}