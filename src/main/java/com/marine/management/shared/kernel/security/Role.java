package com.marine.management.shared.kernel.security;

public enum Role {
    ADMIN("Full system access"),
    MANAGER("Can manage operations"),
    USER("Basic access"),
    CAPTAIN("Vessel operations");

    private final String description;

    Role(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    // === BUSINESS LOGIC (Domain Logic!) ===

    public boolean canManageFinancialEntries() {
        return this == ADMIN || this == MANAGER;
    }

    public boolean canManageCrew() {
        return this == ADMIN || this == CAPTAIN;
    }

    public boolean canViewReports() {
        return this != USER;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }
}
