package com.marine.management.modules.finance.domain.enums;

/**
 * Business document type for financial entry attachments.
 *
 * Used in:
 * - File naming: FE-2024-001_FATURA_01.pdf
 * - UI buttons: "Fatura Ekle", "Fiş Ekle", "Promo Ekle", "Diğer Ekle"
 * - DB column: attachment_type (VARCHAR 20)
 */
public enum AttachmentType {

    /** Invoice / Fatura */
    FATURA,

    /** Receipt / Fiş */
    FIS,

    /** Promotional document / Promosyon belgesi */
    PROMO,

    /** Other documents / Diğer belgeler */
    DIGER
}
