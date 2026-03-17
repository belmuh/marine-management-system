package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.domain.entities.FinancialEntry;
import com.marine.management.modules.finance.domain.entities.FinancialEntryAttachment;
import com.marine.management.modules.finance.domain.enums.AttachmentType;
import com.marine.management.modules.finance.infrastructure.FinancialEntryRepository;
import com.marine.management.modules.finance.presentation.dto.AttachmentResponseDto;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.exceptions.AttachmentNotFoundException;
import com.marine.management.shared.exceptions.EntryNotFoundException;
import com.marine.management.shared.multitenant.TenantContext;
import com.marine.management.shared.security.EntryAccessPolicy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
            AttachmentType attachmentType,
            User uploadedBy
    ) {
        guardTenantContext();

        // Access control
        FinancialEntry entry = findEntryOrThrow(entryId);
        accessPolicy.checkWriteAccess(entry, uploadedBy);

        // Validate file
        validator.validate(file);

        // Compute 1-based sequence for this attachment type within the entry
        int sequence = (int) entry.getAttachments().stream()
                .filter(a -> attachmentType == a.getAttachmentType())
                .count() + 1;

        // Store file and create attachment
        FinancialEntryAttachment attachment = storeAndCreateAttachment(
                file, entry, attachmentType, sequence, uploadedBy
        );

        // Add to entry
        entry.addAttachment(attachment);

        return AttachmentResponseDto.from(attachment);
    }

    public List<AttachmentResponseDto> addAttachments(
            UUID entryId,
            List<MultipartFile> files,
            AttachmentType attachmentType,
            User uploadedBy
    ) {
        return files.stream()
                .map(file -> addAttachment(entryId, file, attachmentType, uploadedBy))
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

    /**
     * Returns a short-lived presigned R2 URL for direct browser download.
     * The file is served from Cloudflare R2, not through this server.
     */
    @Transactional(readOnly = true)
    public String getDownloadUrl(UUID entryId, UUID attachmentId, User currentUser) {
        guardTenantContext();

        FinancialEntry entry = findEntryOrThrow(entryId);
        accessPolicy.checkReadAccess(entry, currentUser);

        FinancialEntryAttachment attachment = findAttachmentInEntry(entry, attachmentId);
        return fileStorageService.getPresignedDownloadUrl(attachment.getFileName());
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
            FinancialEntry entry,
            AttachmentType attachmentType,
            int sequence,
            User uploadedBy
    ) {
        String entryNumber = entry.getEntryNumber().getValue();
        String storedFilename = fileStorageService.storeFile(
                file, entryNumber, attachmentType, sequence
        );

        return FinancialEntryAttachment.create(
                storedFilename,       // R2 object key (used as fileName in DB)
                file.getOriginalFilename(),
                storedFilename,       // filePath = same as key for R2 (no full path needed)
                file.getSize(),
                file.getContentType(),
                attachmentType,
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

}