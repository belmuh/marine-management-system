package com.marine.management.modules.finance.infrastructure.query;

import com.marine.management.modules.finance.domain.enums.RecordType;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Search criteria for FinancialEntry queries.
 *
 * Bu record tüm arama/filtreleme/sıralama parametrelerini tek bir objede toplar.
 *
 * Avantajları:
 * - Method signature'lar temiz kalır (10 parametre yerine 1 obje)
 * - Validation merkezi yapılır
 * - Default değerler tek yerde yönetilir
 * - Immutable - thread-safe
 * - Builder pattern ile kolay kullanım
 */
public record EntrySearchCriteria(
        UUID categoryId,
        RecordType entryType,
        Long whoId,
        Long mainCategoryId,
        LocalDate startDate,
        LocalDate endDate,
        String searchTerm,
        String sortColumn,
        String sortDirection,
        int page,
        int size
) {

    // ============================================
    // DEFAULTS & VALIDATION (Compact Constructor)
    // ============================================

    public EntrySearchCriteria {
        // Sort defaults
        if (sortColumn == null || sortColumn.isBlank()) {
            sortColumn = "entryDate";
        }
        if (sortDirection == null || sortDirection.isBlank()) {
            sortDirection = "desc";
        }

        // Pagination guards
        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > 100) {
            size = 20;
        }

        // Sanitize search term
        if (searchTerm != null) {
            searchTerm = searchTerm.trim();
            if (searchTerm.isEmpty()) {
                searchTerm = null;
            }
        }
    }

    // ============================================
    // BUILDER
    // ============================================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID categoryId;
        private RecordType entryType;
        private Long whoId;
        private Long mainCategoryId;
        private LocalDate startDate;
        private LocalDate endDate;
        private String searchTerm;
        private String sortColumn = "entryDate";
        private String sortDirection = "desc";
        private int page = 0;
        private int size = 20;

        public Builder categoryId(UUID val) {
            this.categoryId = val;
            return this;
        }

        public Builder entryType(RecordType val) {
            this.entryType = val;
            return this;
        }

        public Builder whoId(Long val) {
            this.whoId = val;
            return this;
        }

        public Builder mainCategoryId(Long val) {
            this.mainCategoryId = val;
            return this;
        }

        public Builder startDate(LocalDate val) {
            this.startDate = val;
            return this;
        }

        public Builder endDate(LocalDate val) {
            this.endDate = val;
            return this;
        }

        public Builder dateRange(LocalDate start, LocalDate end) {
            this.startDate = start;
            this.endDate = end;
            return this;
        }

        public Builder searchTerm(String val) {
            this.searchTerm = val;
            return this;
        }

        public Builder sortColumn(String val) {
            this.sortColumn = val;
            return this;
        }

        public Builder sortDirection(String val) {
            this.sortDirection = val;
            return this;
        }

        public Builder sort(String column, String direction) {
            this.sortColumn = column;
            this.sortDirection = direction;
            return this;
        }

        public Builder page(Integer val) {
            this.page = val != null ? val : 0;
            return this;
        }

        public Builder size(Integer val) {
            this.size = val != null ? val : 20;
            return this;
        }

        public Builder pagination(int page, int size) {
            this.page = page;
            this.size = size;
            return this;
        }

        public EntrySearchCriteria build() {
            return new EntrySearchCriteria(
                    categoryId,
                    entryType,
                    whoId,
                    mainCategoryId,
                    startDate,
                    endDate,
                    searchTerm,
                    sortColumn,
                    sortDirection,
                    page,
                    size
            );
        }
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

    public boolean hasTextSearch() {
        return searchTerm != null && !searchTerm.isBlank();
    }

    public boolean hasDateFilter() {
        return startDate != null || endDate != null;
    }

    public boolean hasAnyFilter() {
        return categoryId != null
                || entryType != null
                || whoId != null
                || mainCategoryId != null
                || hasDateFilter()
                || hasTextSearch();
    }
}