package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.domain.enums.AttachmentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Handles file storage via Cloudflare R2 (S3-compatible).
 *
 * When app.r2.enabled=false (local dev), all storage operations run in
 * no-op mode: upload/delete are skipped, download returns a placeholder URL.
 * The app starts and the API works normally — files just aren't persisted.
 *
 * Upload flow  : Spring Boot receives file → streams to R2 bucket.
 * Download flow: Generates a short-lived presigned GET URL → client fetches
 *                directly from R2 (no bandwidth through the app server).
 * Delete flow  : Removes object from R2.
 */
@Service
public class FileStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;
    private final int presignedUrlExpiryMinutes;
    private final boolean r2Enabled;

    public FileStorageService(
            Optional<S3Client> r2Client,
            Optional<S3Presigner> r2Presigner,
            @Value("${app.r2.bucket:marine-attachments}") String bucket,
            @Value("${app.r2.presigned-url-expiry-minutes:60}") int presignedUrlExpiryMinutes,
            @Value("${app.r2.enabled:false}") boolean r2Enabled
    ) {
        this.s3Client = r2Client.orElse(null);
        this.s3Presigner = r2Presigner.orElse(null);
        this.bucket = bucket;
        this.presignedUrlExpiryMinutes = presignedUrlExpiryMinutes;
        this.r2Enabled = r2Enabled;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPLOAD
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Stores a file in R2 with a structured, human-readable key.
     *
     * Key format : {entryNumber}_{attachmentType}_{sequence:02d}_{uuid8}{extension}
     * Example    : FE-2024-001_FATURA_01_a3f9c12b.pdf
     *
     * When R2 is disabled (local dev) the key is generated but upload is skipped.
     *
     * @return the R2 object key (stored in DB as fileName)
     */
    public String storeFile(
            MultipartFile file,
            String entryNumber,
            AttachmentType attachmentType,
            int sequence
    ) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("File extension not allowed: " + extension);
        }

        String safeEntry = entryNumber.replaceAll("[^a-zA-Z0-9\\-]", "_");
        String key = String.format(
                "%s_%s_%02d_%s%s",
                safeEntry,
                attachmentType.name(),
                sequence,
                UUID.randomUUID().toString().substring(0, 8),
                extension
        );

        if (!r2Enabled) {
            // Local dev: skip actual upload, return the generated key for DB storage
            return key;
        }

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, file.getSize()));
        } catch (IOException ex) {
            throw new RuntimeException("Failed to upload file to R2: " + file.getOriginalFilename(), ex);
        }

        return key;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DOWNLOAD (presigned URL)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Generates a time-limited presigned URL for direct R2 download.
     * The URL bypasses the app server — client streams directly from R2.
     * Expires after {@code app.r2.presigned-url-expiry-minutes} (default 60 min).
     *
     * When R2 is disabled (local dev) returns a placeholder string.
     */
    public String getPresignedDownloadUrl(String key) {
        if (!r2Enabled) {
            return "http://localhost/r2-disabled-placeholder/" + key;
        }

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlExpiryMinutes))
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build())
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DELETE
    // ═══════════════════════════════════════════════════════════════════════════

    public void deleteFile(String key) {
        if (!r2Enabled) {
            // Local dev: nothing to delete
            return;
        }

        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VALIDATION (unchanged)
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean isAllowedFileType(String contentType) {
        return ALLOWED_CONTENT_TYPES.contains(contentType);
    }

    public boolean isAllowedExtension(String filename) {
        return ALLOWED_EXTENSIONS.contains(getFileExtension(filename).toLowerCase());
    }

    public boolean isValidFileSize(long size) {
        return size <= MAX_FILE_SIZE_BYTES;
    }

    public long getMaxFileSizeInMB() {
        return MAX_FILE_SIZE_BYTES / (1024 * 1024);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTANTS / HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB

    // Single source of truth — extensions and content-types are derived from this map.
    private static final Map<String, String> EXTENSION_TO_CONTENT_TYPE = Map.of(
            ".pdf",  "application/pdf",
            ".jpg",  "image/jpeg",
            ".jpeg", "image/jpeg",
            ".png",  "image/png",
            ".gif",  "image/gif",
            ".xls",  "application/vnd.ms-excel",
            ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private static final Set<String> ALLOWED_EXTENSIONS    = EXTENSION_TO_CONTENT_TYPE.keySet();
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.copyOf(EXTENSION_TO_CONTENT_TYPE.values());

    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : "";
    }
}
