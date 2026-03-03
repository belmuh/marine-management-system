package com.marine.management.modules.finance.domain.service;

/**
 * Constants used in tree report generation.
 */
public final class TreeReportConstants {

    private TreeReportConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * ID used for items without assigned main category, category, or who reference.
     */
    public static final Long UNASSIGNED_ID = -1L;

    /**
     * Display labels for unassigned items.
     */
    public static final String UNASSIGNED_LABEL_TR = "Atanmamış";
    public static final String UNASSIGNED_LABEL_EN = "Unassigned";

    /**
     * Display labels for unspecified WHO items.
     */
    public static final String WHO_UNSPECIFIED_TR = "Belirtilmemiş";
    public static final String WHO_UNSPECIFIED_EN = "Unspecified";

    /**
     * Percentage calculation scale and rounding.
     */
    public static final int PERCENTAGE_SCALE = 2;
    public static final int PERCENTAGE_DIVISION_SCALE = 4;
}