package com.marine.management.modules.finance.domain.entity;

import com.marine.management.modules.users.domain.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "financial_entry_attachments")
public class FinancialEntryAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private FinancialEntry entry;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    // === CONSTRUCTORS ===
    protected FinancialEntryAttachment() {
        // JPA
    }

    // === FACTORY METHOD ===
    public static FinancialEntryAttachment create(
            String fileName,
            String originalFileName,
            String filePath,
            Long fileSize,
            String contentType,
            User uploadedBy
    ) {
        FinancialEntryAttachment attachment = new FinancialEntryAttachment();
        attachment.fileName = fileName;
        attachment.originalFileName = originalFileName;
        attachment.filePath = filePath;
        attachment.fileSize = fileSize;
        attachment.contentType = contentType;
        attachment.uploadedBy = uploadedBy;
        attachment.uploadedAt = LocalDateTime.now();

        return attachment;
    }

    // === BUSINESS METHODS ===
    public void associateWithEntry(FinancialEntry entry) {
        this.entry = entry;
    }

    public void dissociateFromEntry() {
        this.entry = null;
    }

    // === DOMAIN CHECKS ===
    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }

    public boolean isPdf() {
        return "application/pdf".equalsIgnoreCase(contentType);
    }

    public boolean isDocument() {
        return contentType != null && (
                contentType.startsWith("application/") ||
                        contentType.contains("word") ||
                        contentType.contains("excel") ||
                        contentType.contains("powerpoint")
        );
    }

    public String getFileExtension() {
        if (originalFileName == null) return "";
        int lastDot = originalFileName.lastIndexOf(".");
        return lastDot > 0 ? originalFileName.substring(lastDot + 1).toLowerCase() : "";
    }

    public String getReadableFileSize() {
        if (fileSize == null) return "0 B";

        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }

    // === VALIDATION ===
    public void validate() {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalStateException("File name is required");
        }
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalStateException("Original file name is required");
        }
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalStateException("File path is required");
        }
        if (fileSize == null || fileSize <= 0) {
            throw new IllegalStateException("Valid file size is required");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalStateException("Content type is required");
        }
        if (uploadedBy == null) {
            throw new IllegalStateException("Uploader is required");
        }
    }

    // === GETTERS ===
    public UUID getId() {
        return id;
    }

    public FinancialEntry getEntry() {
        return entry;
    }

    public String getFileName() {
        return fileName;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    // === SETTERS ===
    void setEntry(FinancialEntry entry) {
        this.entry = entry;
    }

    // === EQUALS/HASHCODE ===
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FinancialEntryAttachment)) return false;
        FinancialEntryAttachment that = (FinancialEntryAttachment) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return String.format(
                "FinancialEntryAttachment{id=%s, fileName='%s', size=%s}",
                id, fileName, getReadableFileSize()
        );
    }
}