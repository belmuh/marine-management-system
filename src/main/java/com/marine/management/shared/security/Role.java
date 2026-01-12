package com.marine.management.shared.security;

public enum Role {
    SUPER_ADMIN("All organizations access"),
    ADMIN("Full yacht access"),
    MANAGER("Financial management"),
    CAPTAIN("Vessel operations"),
    USER("Basic crew member");

    private final String description;

    Role(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    // === ENTRY MANAGEMENT ===

    public boolean canCreateEntry() {
        return true;  // Herkes entry girebilir
    }

    public boolean canViewAllEntries() {
        return this != USER;  // USER sadece kendi entries
    }

    public boolean canEditAnyEntry() {
        return this == SUPER_ADMIN || this == ADMIN || this == MANAGER;
    }

    public boolean canDeleteEntry() {
        return this == SUPER_ADMIN || this == ADMIN;
    }

    // === REPORTS ===

    public boolean canViewReports() {
        return this != USER;
    }

    public boolean canViewBudgets() {
        return this == SUPER_ADMIN || this == ADMIN || this == MANAGER;
    }

    // === ADMIN ===

    public boolean isAdmin() {
        return this == ADMIN || this == SUPER_ADMIN;
    }

    public boolean isSuperAdmin() {
        return this == SUPER_ADMIN;
    }

}
