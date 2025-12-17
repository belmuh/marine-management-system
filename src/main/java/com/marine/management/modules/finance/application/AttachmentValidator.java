package com.marine.management.modules.finance.application;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Validator for file attachments.
 * Handles file size, type and security validations.
 */
@Component
public class AttachmentValidator {

    private final FileStorageService fileStorageService;

    public AttachmentValidator(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * Validate a single file
     * @throws IllegalArgumentException if validation fails
     */
    public void validate(MultipartFile file) {
        validateNotNull(file);
        validateNotEmpty(file);
        validateFileSize(file);
        validateFileType(file);
    }

    /**
     * Validate multiple files
     * @throws IllegalArgumentException if any validation fails
     */
    public void validateAll(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files provided");
        }

        files.forEach(this::validate);
    }

    // ============================================
    // VALIDATION RULES
    // ============================================

    private void validateNotNull(MultipartFile file) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
    }

    private void validateNotEmpty(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
    }

    private void validateFileSize(MultipartFile file) {
        if (!fileStorageService.isValidFileSize(file.getSize())) {
            long maxSizeMB = fileStorageService.getMaxFileSizeInMB();
            throw new IllegalArgumentException(
                    String.format("File size exceeds limit of %d MB", maxSizeMB)
            );
        }
    }

    private void validateFileType(MultipartFile file) {
        if (!fileStorageService.isAllowedFileType(file.getContentType())) {
            throw new IllegalArgumentException(
                    String.format("File type '%s' is not allowed", file.getContentType())
            );
        }
    }
}