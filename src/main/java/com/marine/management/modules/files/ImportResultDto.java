package com.marine.management.modules.files;

import java.util.ArrayList;
import java.util.List;

public record ImportResultDto( int totalRows,
                               int successfulRows,
                               int failedRows,
                               int categoriesCreated,
                               int entriesCreated,
                               List<ImportError> errors) {
    public ImportResultDto() {
        this(0, 0, 0, 0, 0, new ArrayList<>());
    }

    public record ImportError(
            int rowNumber,
            String fieldName,
            String errorMessage
    ) {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int totalRows;
        private int successfulRows;
        private int failedRows;
        private int categoriesCreated;
        private int entriesCreated;
        private List<ImportError> errors = new ArrayList<>();

        public Builder totalRows(int totalRows) {
            this.totalRows = totalRows;
            return this;
        }

        public Builder successfulRows(int successfulRows) {
            this.successfulRows = successfulRows;
            return this;
        }

        public Builder failedRows(int failedRows) {
            this.failedRows = failedRows;
            return this;
        }

        public Builder categoriesCreated(int categoriesCreated) {
            this.categoriesCreated = categoriesCreated;
            return this;
        }

        public Builder entriesCreated(int entriesCreated) {
            this.entriesCreated = entriesCreated;
            return this;
        }

        public Builder addError(int rowNumber, String fieldName, String errorMessage) {
            this.errors.add(new ImportError(rowNumber, fieldName, errorMessage));
            this.failedRows++;
            return this;
        }

        public ImportResultDto build() {
            return new ImportResultDto(
                    totalRows,
                    successfulRows,
                    failedRows,
                    categoriesCreated,
                    entriesCreated,
                    errors
            );
        }
    }
}
