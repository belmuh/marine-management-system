package com.marine.management.modules.finance.presentation;

import com.marine.management.modules.finance.application.FileStorageService;
import com.marine.management.modules.finance.application.FinancialEntryService;
import com.marine.management.modules.finance.domain.EntryType;
import com.marine.management.modules.finance.domain.FinancialEntry;
import com.marine.management.modules.finance.domain.FinancialEntryAttachment;
import com.marine.management.modules.finance.domain.Money;
import com.marine.management.modules.finance.infrastructure.FinancialEntryRepository;
import com.marine.management.modules.finance.presentation.dto.EntryResponseDto;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.exceptions.EntryNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/finance/entries")
public class FinancialEntryController {

    private final FinancialEntryService entryService;
    private final FileStorageService fileStorageService;

    public FinancialEntryController(FinancialEntryService entryService, FileStorageService fileStorageService) {
        this.entryService = entryService;
        this.fileStorageService = fileStorageService;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<EntryResponseDto> create(
            @Valid @RequestBody CreateEntryRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        Money amount = Money.of(request.amount(), request.currency());

        FinancialEntry entry = entryService.create(
                request.entryType(),
                request.categoryId(),
                amount,
                request.entryDate(),
                currentUser,
                request.description()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(EntryResponseDto.from(entry));
    }

    @PostMapping("/income")
    public ResponseEntity<EntryResponseDto> createIncome(
            @Valid @RequestBody CreateIncomeExpenseRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        Money amount = Money.of(request.amount(), request.currency());

        FinancialEntry entry = entryService.createIncome(
                request.categoryId(),
                amount,
                request.entryDate(),
                currentUser,
                request.description()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(EntryResponseDto.from(entry));
    }

    @PostMapping("/expense")
    public ResponseEntity<EntryResponseDto> createExpense(
            @Valid @RequestBody CreateIncomeExpenseRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        Money amount = Money.of(request.amount(), request.currency());

        FinancialEntry entry = entryService.createExpense(
                request.categoryId(),
                amount,
                request.entryDate(),
                currentUser,
                request.description()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(EntryResponseDto.from(entry));
    }

    // READ
    @GetMapping("/{id}")
    public ResponseEntity<EntryResponseDto> getById(@PathVariable UUID id) {
        return entryService.findById(id)
                .map(EntryResponseDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{entryNumber}")
    public ResponseEntity<EntryResponseDto> getByEntryNumber(
            @PathVariable String entryNumber
    ) {
        return entryService.findByEntryNumber(entryNumber)
                .map(EntryResponseDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/my-entries")
    public ResponseEntity<Page<EntryResponseDto>> getMyEntries(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20, sort = "entryDate") Pageable pageable
    ) {
        Page<FinancialEntry> entries = entryService.findByUser(currentUser, pageable);
        return ResponseEntity.ok(entries.map(EntryResponseDto::from));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<EntryResponseDto>> getAll(
            @PageableDefault(size = 20, sort = "entryDate") Pageable pageable
    ) {
        Page<FinancialEntry> entries = entryService.findByDateRange(
                LocalDate.now().minusMonths(1),
                LocalDate.now(),
                pageable
        );
        return ResponseEntity.ok(entries.map(EntryResponseDto::from));
    }

    // ============================================
    // UPDATE
    // ============================================

    @PutMapping("/{id}")
    public ResponseEntity<EntryResponseDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEntryRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        Money amount = Money.of(request.amount(), request.currency());

        FinancialEntry entry = entryService.update(
                id,
                request.categoryId(),
                amount,
                request.entryDate(),
                request.description(),
                currentUser
        );

        return ResponseEntity.ok(EntryResponseDto.from(entry));
    }

    @PatchMapping("/{id}/receipt-number")
    public ResponseEntity<EntryResponseDto> updateReceiptNumber(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReceiptNumberRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        FinancialEntry entry = entryService.updateReceiptNumber(
                id,
                request.receiptNumber(),
                currentUser
        );

        return ResponseEntity.ok(EntryResponseDto.from(entry));
    }

    @PatchMapping("/{id}/exchange-rate")
    public ResponseEntity<EntryResponseDto> setExchangeRate(
            @PathVariable UUID id,
            @Valid @RequestBody SetExchangeRateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        FinancialEntry entry = entryService.setExchangeRate(
                id,
                request.rate(),
                request.rateDate(),
                currentUser
        );

        return ResponseEntity.ok(EntryResponseDto.from(entry));
    }

    // ============================================
    // DELETE
    // ============================================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        entryService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    // ============================================
    // SEARCH
    // ============================================

    @GetMapping("/search")
    public ResponseEntity<Page<EntryResponseDto>> search(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) EntryType entryType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<FinancialEntry> entries = entryService.search(
                categoryId,
                entryType,
                startDate,
                endDate,
                pageable
        );

        return ResponseEntity.ok(entries.map(EntryResponseDto::from));
    }

    @GetMapping("/search/text")
    public ResponseEntity<Page<EntryResponseDto>> searchByText(
            @RequestParam String searchTerm,
            @RequestParam(required = false) EntryType entryType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<FinancialEntry> entries = entryService.searchByText(
                searchTerm,
                entryType,
                startDate,
                endDate,
                pageable
        );

        return ResponseEntity.ok(entries.map(EntryResponseDto::from));
    }

    // ============================================
    // DASHBOARD & STATISTICS
    // ============================================

    @GetMapping("/dashboard/summary")
    public ResponseEntity<FinancialEntryService.DashboardSummary> getDashboardSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        FinancialEntryService.DashboardSummary summary =
                entryService.getDashboardSummary(start, end);

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/dashboard/period-totals")
    public ResponseEntity<List<FinancialEntryRepository.PeriodTotalProjection>> getPeriodTotals(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(entryService.getPeriodTotals(startDate, endDate));
    }

    @GetMapping("/dashboard/category-totals")
    public ResponseEntity<List<FinancialEntryRepository.CategoryTotalProjection>> getCategoryTotals(
            @RequestParam EntryType entryType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(
                entryService.getCategoryTotals(entryType, startDate, endDate)
        );
    }

    @GetMapping("/dashboard/expense-totals")
    public ResponseEntity<List<FinancialEntryRepository.CategoryTotalProjection>> getExpenseTotals(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(entryService.getExpenseTotals(startDate, endDate));
    }

    @GetMapping("/dashboard/income-totals")
    public ResponseEntity<List<FinancialEntryRepository.CategoryTotalProjection>> getIncomeTotals(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(entryService.getIncomeTotals(startDate, endDate));
    }

    @GetMapping("/dashboard/monthly-totals")
    public ResponseEntity<List<FinancialEntryRepository.MonthlyTotalProjection>> getMonthlyTotals(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(entryService.getMonthlyTotals(startDate, endDate));
    }

    // ATTACHMENT

    @PostMapping("/{id}/attachments")
    public ResponseEntity<AttachmentResponseDto> addAttachment(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser
    ) {
        // Validation
        if (!fileStorageService.isValidFileSize(file.getSize())) {
            throw new IllegalArgumentException("File too large");
        }

        if (!fileStorageService.isAllowedFileType(file.getContentType())) {
            throw new IllegalArgumentException("File type not allowed");
        }

        // Store file
        String storedFilename = fileStorageService.storeFile(file);

        // Create attachment entity
        FinancialEntryAttachment attachment = new FinancialEntryAttachment();
        attachment.setFileName(storedFilename);
        attachment.setOriginalFileName(file.getOriginalFilename());
        attachment.setFilePath(fileStorageService.getFileStorageLocation() + "/" + storedFilename);
        attachment.setFileSize(file.getSize());
        attachment.setContentType(file.getContentType());
        attachment.setUploadedBy(currentUser);
        attachment.setUploadedAt(LocalDateTime.now());

        // Add to entry
        FinancialEntry entry = entryService.addAttachment(id, attachment, currentUser);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AttachmentResponseDto.from(attachment));
    }

    @GetMapping("/{id}/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable UUID id,
            @PathVariable UUID attachmentId
    ) {
        // Get attachment
        FinancialEntry entry = entryService.findById(id)
                .orElseThrow(() -> EntryNotFoundException.withId(id));

        FinancialEntryAttachment attachment = entry.getAttachments().stream()
                .filter(a -> a.getId().equals(attachmentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));

        // Load file
        Resource resource = fileStorageService.loadFileAsResource(attachment.getFileName());

        // Return file
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getOriginalFileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, attachment.getContentType())
                .body(resource);
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<Void> removeAttachment(
            @PathVariable UUID id,
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal User currentUser
    ) {
        // Get attachment before deleting
        FinancialEntry entry = entryService.findById(id)
                .orElseThrow(() -> EntryNotFoundException.withId(id));

        FinancialEntryAttachment attachment = entry.getAttachments().stream()
                .filter(a -> a.getId().equals(attachmentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));

        // Remove from entry (domain)
        entryService.removeAttachment(id, attachmentId, currentUser);

        // Delete physical file
        fileStorageService.deleteFile(attachment.getFileName());

        return ResponseEntity.noContent().build();
    }

    // DTOS
    public record CreateEntryRequest(
            @NotNull EntryType entryType,
            @NotNull UUID categoryId,
            @NotBlank String amount,
            @NotBlank String currency,
            @NotNull LocalDate entryDate,
            String description
    ) {}

    public record CreateIncomeExpenseRequest(
            @NotNull UUID categoryId,
            @NotBlank String amount,
            @NotBlank String currency,
            @NotNull LocalDate entryDate,
            String description
    ) {}

    public record UpdateEntryRequest(
            @NotNull UUID categoryId,
            @NotBlank String amount,
            @NotBlank String currency,
            @NotNull LocalDate entryDate,
            String description
    ) {}

    public record UpdateReceiptNumberRequest(
            String receiptNumber
    ) {}

    public record SetExchangeRateRequest(
            @NotNull BigDecimal rate,
            @NotNull LocalDate rateDate
    ) {}

    public record MoneyDto(String amount, String currency) {
        public static MoneyDto from(Money money) {
            return new MoneyDto(
                    money.amount().toString(),
                    money.currencyCode()
            );
        }
    }

    // AttachmentResponseDto
    public record AttachmentResponseDto(
            UUID id,
            String fileName,
            String originalFileName,
            Long fileSize,
            String contentType,
            LocalDateTime uploadedAt
    ) {
        public static AttachmentResponseDto from(FinancialEntryAttachment attachment) {
            return new AttachmentResponseDto(
                    attachment.getId(),
                    attachment.getFileName(),
                    attachment.getOriginalFileName(),
                    attachment.getFileSize(),
                    attachment.getContentType(),
                    attachment.getUploadedAt()
            );
        }
    }
}
