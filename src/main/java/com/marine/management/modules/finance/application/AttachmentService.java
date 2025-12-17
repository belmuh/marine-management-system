package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.domain.entity.FinancialEntry;
import com.marine.management.modules.finance.domain.entity.FinancialEntryAttachment;
import com.marine.management.modules.finance.presentation.dto.AttachmentResponseDto;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.exceptions.AttachmentNotFoundException;
import com.marine.management.shared.exceptions.EntryNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing financial entry attachments.
 * Handles file upload, download, validation and deletion.
 */
@Service
@Transactional
public class AttachmentService {

    private final FinancialEntryService entryService;
    private final FileStorageService fileStorageService;
    private final AttachmentValidator validator;

    public AttachmentService(
            FinancialEntryService entryService,
            FileStorageService fileStorageService,
            AttachmentValidator validator
    ) {
        this.entryService = entryService;
        this.fileStorageService = fileStorageService;
        this.validator = validator;
    }

    // ============================================
    // PUBLIC API
    // ============================================

    /**
     * Add a single attachment to an entry
     */
    public AttachmentResponseDto addAttachment(
            UUID entryId,
            MultipartFile file,
            User uploadedBy
    ) {
        // 1. Validate file
        validator.validate(file);

        // 2. Store file
        FinancialEntryAttachment attachment = storeAndCreateAttachment(file, uploadedBy);

        // 3. Add to entry
        var command = new FinancialEntryService.AddAttachmentCommand(
                entryId,
                attachment,
                uploadedBy
        );
        entryService.addAttachment(command);

        return AttachmentResponseDto.from(attachment);
    }

    /**
     * Add multiple attachments to an entry
     */
    public List<AttachmentResponseDto> addAttachments(
            UUID entryId,
            List<MultipartFile> files,
            User uploadedBy
    ) {
        return files.stream()
                .map(file -> addAttachment(entryId, file, uploadedBy))
                .collect(Collectors.toList());
    }

    /**
     * Download an attachment
     */
    public ResponseEntity<Resource> downloadAttachment(
            UUID entryId,
            UUID attachmentId
    ) {
        var attachment = findAttachmentOrThrow(entryId, attachmentId);
        var resource = fileStorageService.loadFileAsResource(attachment.getFileName());

        return createFileDownloadResponse(resource, attachment);
    }

    /**
     * Remove an attachment from entry and delete file
     */
    public void removeAttachment(
            UUID entryId,
            UUID attachmentId,
            User requestedBy
    ) {
        var attachment = findAttachmentOrThrow(entryId, attachmentId);

        // Remove from entry
        var command = new FinancialEntryService.RemoveAttachmentCommand(
                entryId,
                attachmentId,
                requestedBy
        );
        entryService.removeAttachment(command);

        // Delete file from storage
        fileStorageService.deleteFile(attachment.getFileName());
    }

    /**
     * Get all attachments for an entry
     */
    @Transactional(readOnly = true)
    public List<AttachmentResponseDto> getAttachments(UUID entryId) {
        var entry = findEntryOrThrow(entryId);

        return entry.getAttachments().stream()
                .map(AttachmentResponseDto::from)
                .collect(Collectors.toList());
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private FinancialEntryAttachment storeAndCreateAttachment(
            MultipartFile file,
            User uploadedBy
    ) {
        String storedFilename = fileStorageService.storeFile(file);

        return FinancialEntryAttachment.create(
                storedFilename,
                file.getOriginalFilename(),
                fileStorageService.getFileStorageLocation() + "/" + storedFilename,
                file.getSize(),
                file.getContentType(),
                uploadedBy
        );
    }

    private FinancialEntry findEntryOrThrow(UUID id) {
        return entryService.findById(id)
                .orElseThrow(() -> EntryNotFoundException.withId(id));
    }

    private FinancialEntryAttachment findAttachmentOrThrow(UUID entryId, UUID attachmentId) {
        var entry = findEntryOrThrow(entryId);

        return entry.getAttachments().stream()
                .filter(a -> a.getId().equals(attachmentId))
                .findFirst()
                .orElseThrow(() -> AttachmentNotFoundException.withId(attachmentId));
    }

    private ResponseEntity<Resource> createFileDownloadResponse(
            Resource resource,
            FinancialEntryAttachment attachment
    ) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getOriginalFileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, attachment.getContentType())
                .body(resource);
    }
}