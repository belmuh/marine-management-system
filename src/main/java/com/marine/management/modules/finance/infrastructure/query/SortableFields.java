package com.marine.management.modules.finance.infrastructure.query;

import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * Sort field whitelist and mapping for FinancialEntry queries.
 *
 * Bu class iki kritik görevi yerine getirir:
 *
 * 1. SECURITY (Whitelist):
 *    - Sadece izin verilen kolonlarda sort yapılabilir
 *    - Frontend'den gelen rastgele field adları kabul edilmez
 *    - SQL injection'a karşı koruma sağlar
 *
 * 2. MAPPING (Frontend → Entity):
 *    - Frontend "baseAmount" gönderir → Backend "baseAmount.amount" kullanır
 *    - Frontend "categoryName" gönderir → Backend "category.name" kullanır
 *    - DTO field adları ile Entity field adları arasındaki farkı çözer
 *
 * Neden enum yerine class?
 *    - Switch expression ile flexible mapping
 *    - Static utility methods
 *    - Kolay test edilebilirlik
 */
public final class SortableFields {

    private SortableFields() {
        // Utility class - instance oluşturulamaz
    }

    // ============================================
    // WHITELIST - Sadece bunlar sort edilebilir
    // ============================================

    private static final Set<String> ALLOWED_ENTITY_PATHS = Set.of(
            "entryDate",
            "entryNumber.value",
            "baseAmount.amount",
            "originalAmount.amount",
            "category.name",
            "createdAt",
            "status",
            "paymentMethod"
    );

    // ============================================
    // DEFAULTS
    // ============================================

    public static final String DEFAULT_SORT_COLUMN = "entryDate";
    public static final Sort.Direction DEFAULT_DIRECTION = Sort.Direction.DESC;

    // ============================================
    // PUBLIC API
    // ============================================

    /**
     * Frontend parametrelerinden Spring Sort objesi oluşturur.
     *
     * @param column Frontend'den gelen kolon adı (örn: "baseAmount", "categoryName")
     * @param direction "asc" veya "desc"
     * @return Spring Data Sort objesi
     */
    public static Sort createSort(String column, String direction) {
        String entityPath = toEntityPath(column);
        Sort.Direction sortDirection = parseDirection(direction);
        return Sort.by(sortDirection, entityPath);
    }

    /**
     * Verilen kolon adının sort edilebilir olup olmadığını kontrol eder.
     */
    public static boolean isSortable(String column) {
        if (column == null || column.isBlank()) {
            return false;
        }
        String entityPath = toEntityPath(column);
        return ALLOWED_ENTITY_PATHS.contains(entityPath);
    }

    /**
     * İzin verilen tüm sort field'larını döner.
     * API documentation için kullanılabilir.
     */
    public static Set<String> getAllowedFields() {
        return Set.copyOf(ALLOWED_ENTITY_PATHS);
    }

    // ============================================
    // MAPPING LOGIC
    // ============================================

    /**
     * Frontend kolon adını Entity path'ine çevirir.
     *
     * Mapping örnekleri:
     * - "baseAmount" → "baseAmount.amount" (Embedded Money objesi)
     * - "categoryName" → "category.name" (JOIN relation)
     * - "entryNumber" → "entryNumber.value" (Value Object)
     *
     * Bilinmeyen kolonlar DEFAULT_SORT_COLUMN'a fallback eder.
     */
    private static String toEntityPath(String requestParam) {
        if (requestParam == null || requestParam.isBlank()) {
            return DEFAULT_SORT_COLUMN;
        }

        // Normalize: trim ve lowercase
        String normalized = requestParam.trim().toLowerCase();

        return switch (normalized) {
            // Date fields
            case "entrydate", "entry_date", "date"
                    -> "entryDate";

            // Entry number (Value Object)
            case "entrynumber", "entry_number", "number"
                    -> "entryNumber.value";

            // Money fields (Embedded)
            case "baseamount", "base_amount", "amount", "baseamount.amount"
                    -> "baseAmount.amount";

            case "originalamount", "original_amount", "originalamount.amount"
                    -> "originalAmount.amount";

            // Category (JOIN)
            case "categoryname", "category_name", "category.name", "category"
                    -> "category.name";

            // Audit fields
            case "createdat", "created_at", "created"
                    -> "createdAt";

            // Status
            case "status"
                    -> "status";

            // Status
            case "paymentmethod"
                    -> "paymentMethod";

            // Unknown → fallback to default (SECURITY)
            default -> {
                // Eğer gelen değer zaten whitelist'te varsa, olduğu gibi kullan
                if (ALLOWED_ENTITY_PATHS.contains(requestParam)) {
                    yield requestParam;
                }
                // Bilinmeyen field → güvenli default'a dön
                yield DEFAULT_SORT_COLUMN;
            }
        };
    }

    /**
     * Direction string'ini Sort.Direction'a çevirir.
     * Geçersiz değerler DESC'e fallback eder.
     */
    private static Sort.Direction parseDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            return DEFAULT_DIRECTION;
        }

        return "asc".equalsIgnoreCase(direction.trim())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
    }
}