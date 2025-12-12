package com.marine.management.modules.finance.presentation;

import com.marine.management.modules.finance.application.FileStorageService;
import com.marine.management.modules.finance.application.FinancialEntryService;
import com.marine.management.modules.finance.domain.entity.FinancialEntry;
import com.marine.management.modules.finance.domain.entity.FinancialEntryAttachment;
import com.marine.management.modules.finance.presentation.dto.*;
import com.marine.management.modules.finance.presentation.dto.controller.*;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.exceptions.AttachmentNotFoundException;
import com.marine.management.shared.exceptions.EntryNotFoundException;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.marine.management.modules.finance.presentation.dto.controller.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/finance/entries")
public class FinancialEntryController {

    private final FinancialEntryService entryService;
    private final FileStorageService fileStorageService;
    private final EntryRequestMapper requestMapper;

    public FinancialEntryController(
            FinancialEntryService entryService,
            FileStorageService fileStorageService
    ) {
        this.entryService = entryService;
        this.fileStorageService = fileStorageService;
        this.requestMapper = new EntryRequestMapper();
    }

    // ============================================
    // CREATE OPERATION
    // ============================================

    @PostMapping
    public ResponseEntity<EntryResponseDto> createEntry(
            @Valid @RequestBody CreateEntryRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        var command = requestMapper.toCreateEntryCommand(request, currentUser);
        var entry = entryService.createEntry(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(EntryResponseDto.from(entry));
    }

    // ============================================
    // READ OPERATIONS
    // ============================================

    @GetMapping("/{id}")
    public ResponseEntity<EntryResponseDto> getById(@PathVariable UUID id) {
        var entry = findEntryOrThrow(id);
        return ResponseEntity.ok(EntryResponseDto.from(entry));
    }

    @GetMapping("/number/{entryNumber}")
    public ResponseEntity<EntryResponseDto> getByEntryNumber(
            @PathVariable String entryNumber
    ) {
        var entry = entryService.findByEntryNumber(entryNumber)
                .orElseThrow(() -> EntryNotFoundException.withEntryNumber(entryNumber));

        return ResponseEntity.ok(EntryResponseDto.from(entry));
    }

    @GetMapping("/my-entries")
    public ResponseEntity<Page<EntryResponseDto>> getMyEntries(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20, sort = "entryDate") Pageable pageable
    ) {
        var entries = entryService.findByUser(currentUser, pageable);
        return ResponseEntity.ok(entries.map(EntryResponseDto::from));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<EntryResponseDto>> getAll(
            @PageableDefault(size = 20, sort = "entryDate") Pageable pageable
    ) {
        var entries = entryService.findByDateRange(
                LocalDate.now().minusMonths(1),
                LocalDate.now(),
                pageable
        );
        return ResponseEntity.ok(entries.map(EntryResponseDto::from));
    }

    // ============================================
    // UPDATE OPERATIONS
    // ============================================

    @PutMapping("/{id}")
    public ResponseEntity<EntryResponseDto> updateEntry(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEntryRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        var command = requestMapper.toUpdateEntryCommand(id, request, currentUser);
        var entry = entryService.updateEntry(command);

        return ResponseEntity.ok(EntryResponseDto.from(entry));
    }

    @PatchMapping("/{id}/context")
    public ResponseEntity<EntryResponseDto> updateEntryContext(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEntryContextRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        var command = new FinancialEntryService.UpdateEntryContextCommand(
                id,
                request.whoId(),
                request.mainCategoryId(),
                request.recipient(),
                request.country(),
                request.city(),
                request.specificLocation(),
                request.vendor(),
                currentUser
        );
        var entry = entryService.updateEntryContext(command);

        return ResponseEntity.ok(EntryResponseDto.from(entry));
    }

    @PatchMapping("/{id}/metadata")
    public ResponseEntity<EntryResponseDto> updateEntryMetadata(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEntryMetadataRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        var command = new FinancialEntryService.UpdateEntryMetadataCommand(
                id,
                request.frequency(),
                request.priority(),
                request.tags(),
                currentUser
        );
        var entry = entryService.updateEntryMetadata(command);

        return ResponseEntity.ok(EntryResponseDto.from(entry));
    }

    @PatchMapping("/{id}/receipt-number")
    public ResponseEntity<EntryResponseDto> updateReceiptNumber(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReceiptNumberRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        var command = new FinancialEntryService.UpdateReceiptNumberCommand(
                id, request.receiptNumber(), currentUser
        );
        var entry = entryService.updateReceiptNumber(command);

        return ResponseEntity.ok(EntryResponseDto.from(entry));
    }

    @PatchMapping("/{id}/exchange-rate")
    public ResponseEntity<EntryResponseDto> updateExchangeRate(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExchangeRateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        var command = new FinancialEntryService.UpdateExchangeRateCommand(
                id, request.rate(), request.rateDate(), currentUser
        );
        var entry = entryService.updateExchangeRate(command);

        return ResponseEntity.ok(EntryResponseDto.from(entry));
    }

    // ============================================
    // DELETE OPERATIONS
    // ============================================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntry(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        var command = new FinancialEntryService.DeleteEntryCommand(id, currentUser);
        entryService.deleteEntry(command);
        return ResponseEntity.noContent().build();
    }

    // ============================================
    // SEARCH OPERATIONS
    // ============================================

    @GetMapping("/search")
    public ResponseEntity<Page<EntryResponseDto>> searchEntries(
            @Valid EntrySearchRequest request,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        var criteria = new FinancialEntryService.EntrySearchCriteria(
                request.categoryId(),
                request.entryType(),
                request.whoId(),
                request.mainCategoryId(),
                request.startDate(),
                request.endDate()
        );
        var entries = entryService.search(criteria, pageable);

        return ResponseEntity.ok(entries.map(EntryResponseDto::from));
    }

    @GetMapping("/search/text")
    public ResponseEntity<Page<EntryResponseDto>> searchByText(
            @Valid TextSearchRequest request,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        var criteria = new FinancialEntryService.TextSearchCriteria(
                request.searchTerm(),
                request.entryType(),
                request.startDate(),
                request.endDate()
        );
        var entries = entryService.searchByText(criteria, pageable);

        return ResponseEntity.ok(entries.map(EntryResponseDto::from));
    }

    // ============================================
    // ATTACHMENT OPERATIONS
    // ============================================

    @PostMapping("/{id}/attachments")
    public ResponseEntity<AttachmentResponseDto> addAttachment(
            @PathVariable UUID id,
            @Valid @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser
    ) {
        validateFile(file);
        FinancialEntryAttachment attachment = createAttachment(file, currentUser);

        var command = new FinancialEntryService.AddAttachmentCommand(id, attachment, currentUser);
        entryService.addAttachment(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AttachmentResponseDto.from(attachment));
    }

    @GetMapping("/{id}/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable UUID id,
            @PathVariable UUID attachmentId
    ) {
        var attachment = findAttachmentOrThrow(id, attachmentId);
        var resource = fileStorageService.loadFileAsResource(attachment.getFileName());

        return createFileDownloadResponse(resource, attachment);
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<Void> removeAttachment(
            @PathVariable UUID id,
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal User currentUser
    ) {
        var attachment = findAttachmentOrThrow(id, attachmentId);
        var command = new FinancialEntryService.RemoveAttachmentCommand(id, attachmentId, currentUser);

        entryService.removeAttachment(command);
        fileStorageService.deleteFile(attachment.getFileName());

        return ResponseEntity.noContent().build();
    }

    // ============================================
    // HELPER METHODS
    // ============================================

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

    private void validateFile(MultipartFile file) {
        if (!fileStorageService.isValidFileSize(file.getSize())) {
            throw new IllegalArgumentException("File size exceeds limit");
        }
        if (!fileStorageService.isAllowedFileType(file.getContentType())) {
            throw new IllegalArgumentException("File type not allowed");
        }
    }

    private FinancialEntryAttachment createAttachment(MultipartFile file, User uploadedBy) {
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

    // ============================================
    // INNER CLASS - REQUEST MAPPER
    // ============================================

    private static class EntryRequestMapper {

        FinancialEntryService.CreateEntryCommand toCreateEntryCommand(
                CreateEntryRequest request,
                User creator
        ) {
            var moneyDto = new MoneyDto(request.amount(), request.currency());
            var amount = moneyDto.toMoney();

            return new FinancialEntryService.CreateEntryCommand(
                    request.entryType(),
                    request.categoryId(),
                    amount,
                    request.entryDate(),
                    request.paymentMethod(),
                    request.description(),
                    creator,
                    request.whoId(),
                    request.mainCategoryId(),
                    request.recipient(),
                    request.country(),
                    request.city(),
                    request.specificLocation(),
                    request.vendor()
            );
        }

        FinancialEntryService.UpdateEntryCommand toUpdateEntryCommand(
                UUID entryId,
                UpdateEntryRequest request,
                User updater
        ) {
            var moneyDto = new MoneyDto(request.amount(), request.currency());
            var amount = moneyDto.toMoney();

            return new FinancialEntryService.UpdateEntryCommand(
                    entryId,
                    request.entryType(),
                    request.categoryId(),
                    amount,
                    request.entryDate(),
                    request.paymentMethod(),
                    request.description(),
                    updater
            );
        }
    }
}

// ============================================
// REQUEST DTOs (Updated with who and mainCategory)
// ============================================

