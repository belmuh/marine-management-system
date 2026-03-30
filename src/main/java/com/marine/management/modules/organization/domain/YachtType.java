package com.marine.management.modules.organization.domain;

/**
 * Type of yacht managed by the organization.
 *
 * Used during onboarding registration and for reporting/filtering.
 * Stored as STRING in DB (@Enumerated(EnumType.STRING)).
 */
public enum YachtType {

    MOTOR_YACHT("Motor Yacht"),
    SAILING_YACHT("Sailing Yacht"),
    CATAMARAN("Catamaran"),
    GULET("Gulet"),
    OTHER("Other");

    private final String displayName;

    YachtType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
