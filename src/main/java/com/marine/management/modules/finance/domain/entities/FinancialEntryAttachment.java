package com.marine.management.modules.finance.domain.entities;

import com.marine.management.modules.users.domain.User;
import jakarta.persistence.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Financial entry attachment entity.
 *
 * TENANT ISOLATION:
 * - Does NOT extend BaseTenantEntity
 * - Tenant isolation through parent FinancialEntry
 * - Cascade operations inherit tenant context
 *
 * WHY NO BaseTenantEntity?
 * - Attachment is child aggregate of FinancialEntry
 * - Always accessed through parent entry
 * - Never queried independently
 * - Tenant isolation via parent's tenant_id
 *
 * @see FinancialEntry
 */
@Entity
@Audited
@Table(
        name = "financial_entry_attachments",
        indexes = {
                @Index(name = "idx_attachments_entry", columnList = "entry_id"),
                @Index(name = "idx_attachments_uploaded_by", columnList = "uploaded_by")
        }
)
public class FinancialEntryAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    /**
     * Parent financial entry.
     *
     * TENANT ISOLATION:
     * - Entry has tenant_id
     * - Attachment inherits tenant context through this relationship
     * - Cascade operations maintain tenant isolation
     */
    @NotAudited
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private FinancialEntry entry;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @NotAudited
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    // === CONSTRUCTORS ===
    protected FinancialEntryAttachment() {
        // JPA
    }

    // === FACTORY METHOD ===
    /**
     * Creates a new attachment.
     *
     * NOTE: Attachment is NOT persisted until associated with entry.
     * Entry association happens via entry.addAttachment().
     */
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

        attachment.validate();

        return attachment;
    }

    // === BUSINESS METHODS ===

    /**
     * Associates attachment with parent entry.
     *
     * Called by FinancialEntry.addAttachment().
     */
    public void associateWithEntry(FinancialEntry entry) {
        this.entry = entry;
    }

    /**
     * Dissociates attachment from parent entry.
     *
     * Called by FinancialEntry.removeAttachment().
     */
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

    // === SETTERS (Package-private) ===

    void setEntry(FinancialEntry entry) {
        this.entry = entry;
    }

    // === EQUALS/HASHCODE ===

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FinancialEntryAttachment that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return String.format(
                "FinancialEntryAttachment{id=%s, fileName='%s', size=%s}",
                id, fileName, getReadableFileSize()
        );
    }
}