package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.domain.entities.FinancialEntry;
import com.marine.management.modules.finance.domain.entities.FinancialEntryAttachment;
import com.marine.management.modules.finance.infrastructure.FinancialEntryRepository;
import com.marine.management.modules.finance.presentation.dto.AttachmentResponseDto;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.exceptions.AttachmentNotFoundException;
import com.marine.management.shared.exceptions.EntryNotFoundException;
import com.marine.management.shared.multitenant.TenantContext;
import com.marine.management.shared.security.EntryAccessPolicy;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AttachmentService {

    private final FinancialEntryRepository entryRepository;  // 🆕 Direct repository access
    private final FileStorageService fileStorageService;
    private final AttachmentValidator validator;
    private final EntryAccessPolicy accessPolicy;  // 🆕 Access control

    public AttachmentService(
            FinancialEntryRepository entryRepository,
            FileStorageService fileStorageService,
            AttachmentValidator validator,
            EntryAccessPolicy accessPolicy
    ) {
        this.entryRepository = entryRepository;
        this.fileStorageService = fileStorageService;
        this.validator = validator;
        this.accessPolicy = accessPolicy;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════

    public AttachmentResponseDto addAttachment(
            UUID entryId,
            MultipartFile file,
            User uploadedBy
    ) {
        guardTenantContext();

        // Access control
        FinancialEntry entry = findEntryOrThrow(entryId);
        accessPolicy.checkWriteAccess(entry, uploadedBy);

        // Validate file
        validator.validate(file);

        // Store file and create attachment
        FinancialEntryAttachment attachment = storeAndCreateAttachment(file, uploadedBy);

        // Add to entry
        entry.addAttachment(attachment);

        return AttachmentResponseDto.from(attachment);
    }

    public List<AttachmentResponseDto> addAttachments(
            UUID entryId,
            List<MultipartFile> files,
            User uploadedBy
    ) {
        return files.stream()
                .map(file -> addAttachment(entryId, file, uploadedBy))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponseDto> getAttachments(UUID entryId, User currentUser) {
        guardTenantContext();

        FinancialEntry entry = findEntryOrThrow(entryId);
        accessPolicy.checkReadAccess(entry, currentUser);  // 🆕 Access control

        return entry.getAttachments().stream()
                .map(AttachmentResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadAttachment(
            UUID entryId,
            UUID attachmentId,
            User currentUser  // 🆕 User parametresi
    ) {
        guardTenantContext();

        FinancialEntry entry = findEntryOrThrow(entryId);
        accessPolicy.checkReadAccess(entry, currentUser);  // 🆕 Access control

        FinancialEntryAttachment attachment = findAttachmentInEntry(entry, attachmentId);
        Resource resource = fileStorageService.loadFileAsResource(attachment.getFileName());

        return createFileDownloadResponse(resource, attachment);
    }

    public void removeAttachment(
            UUID entryId,
            UUID attachmentId,
            User requestedBy
    ) {
        guardTenantContext();

        FinancialEntry entry = findEntryOrThrow(entryId);
        accessPolicy.checkWriteAccess(entry, requestedBy);  // 🆕 Access control

        FinancialEntryAttachment attachment = findAttachmentInEntry(entry, attachmentId);

        // Remove from entry
        entry.removeAttachment(attachment);

        // Delete file from storage
        fileStorageService.deleteFile(attachment.getFileName());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

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

    private void guardTenantContext() {
        if (!TenantContext.hasTenantContext()) {
            throw new AccessDeniedException("No tenant context available");
        }
    }

    private FinancialEntry findEntryOrThrow(UUID id) {
        return entryRepository.findById(id)
                .orElseThrow(() -> EntryNotFoundException.withId(id));
    }

    private FinancialEntryAttachment findAttachmentInEntry(FinancialEntry entry, UUID attachmentId) {
        return entry.getAttachments().stream()
                .filter(a -> a.getId().equals(attachmentId))
                .findFirst()
                .orElseThrow(() -> AttachmentNotFoundException.withId(attachmentId));
    }

    private ResponseEntity<Resource> createFileDownloadResponse(
            Resource resource,
            FinancialEntryAttachment attachment
    ) {
        String encodedFilename;
        try {
            encodedFilename = URLEncoder.encode(
                    attachment.getOriginalFileName(),
                    StandardCharsets.UTF_8
            ).replace("+", "%20");
        } catch (Exception e) {
            encodedFilename = attachment.getOriginalFileName()
                    .replaceAll("[^a-zA-Z0-9._-]", "_");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFilename)
                .header(HttpHeaders.CONTENT_TYPE, attachment.getContentType())
                .body(resource);
    }
}