package com.marine.management.modules.finance.presentation;

import com.marine.management.modules.finance.application.FinancialEntryService;
import com.marine.management.modules.finance.application.mapper.EntryRequestMapper;
import com.marine.management.modules.finance.domain.enums.EntryStatus;
import com.marine.management.modules.finance.presentation.dto.*;
import com.marine.management.modules.finance.presentation.dto.controller.*;
import com.marine.management.modules.users.domain.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/finance/entries")
public class FinancialEntryController {

    private final FinancialEntryService entryService;
    private final EntryRequestMapper requestMapper;

    public FinancialEntryController(
            FinancialEntryService entryService,
            EntryRequestMapper requestMapper
    ) {
        this.entryService = entryService;
        this.requestMapper = requestMapper;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CREATE
    // ═══════════════════════════════════════════════════════════════════════════

    @PostMapping
    @PreAuthorize("hasAuthority('ENTRY_CREATE')")
    public ResponseEntity<EntryResponseDto> createEntry(
            @Valid @RequestBody CreateEntryRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        var command = requestMapper.toCreateEntryCommand(request, currentUser);
        var dto = entryService.createEntry(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // READ - Single Entry (access checked in service)
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntryResponseDto> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(entryService.getById(id, currentUser));
    }

    @GetMapping("/number/{entryNumber}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntryResponseDto> getByEntryNumber(
            @PathVariable String entryNumber,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(entryService.getByEntryNumber(entryNumber, currentUser));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SEARCH - Lists (role-based filtering in service)
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/expenses/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<EntryResponseDto>> searchExpenses(
            @Valid EntrySearchRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        var criteria = requestMapper.toSearchCriteria(request);
        return ResponseEntity.ok(entryService.searchExpenses(criteria, currentUser));
    }

    @GetMapping("/incomes/search")
    @PreAuthorize("hasAuthority('INCOME_VIEW')")
    public ResponseEntity<Page<EntryResponseDto>> searchIncomes(
            @Valid EntrySearchRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        var criteria = requestMapper.toSearchCriteria(request);
        return ResponseEntity.ok(entryService.searchIncomes(criteria, currentUser));
    }

    // Legacy endpoint - delegates to searchExpenses
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<EntryResponseDto>> search(
            @Valid EntrySearchRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        var criteria = requestMapper.toSearchCriteria(request);
        return ResponseEntity.ok(entryService.search(criteria, currentUser));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STATUS-BASED QUERIES
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('ENTRY_VIEW_ALL')")
    public ResponseEntity<Page<EntryResponseDto>> getByStatus(
            @PathVariable EntryStatus status,
            @PageableDefault(size = 20, sort = "entryDate") Pageable pageable
    ) {
        return ResponseEntity.ok(entryService.findByStatus(status, pageable));
    }

    @GetMapping("/status/count/{status}")
    @PreAuthorize("hasAuthority('ENTRY_VIEW_ALL')")
    public ResponseEntity<Long> countByStatus(@PathVariable EntryStatus status) {
        return ResponseEntity.ok(entryService.countByStatus(status));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE (access checked in service)
    // ═══════════════════════════════════════════════════════════════════════════

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntryResponseDto> updateEntry(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEntryRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        var command = requestMapper.toUpdateEntryCommand(id, request, currentUser);
        return ResponseEntity.ok(entryService.updateEntry(command));
    }

    @PatchMapping("/{id}/context")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntryResponseDto> updateEntryContext(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEntryContextRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        var command = requestMapper.toUpdateEntryContextCommand(id, request, currentUser);
        return ResponseEntity.ok(entryService.updateEntryContext(command));
    }

    @PatchMapping("/{id}/metadata")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntryResponseDto> updateEntryMetadata(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEntryMetadataRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        var command = requestMapper.toUpdateEntryMetadataCommand(id, request, currentUser);
        return ResponseEntity.ok(entryService.updateEntryMetadata(command));
    }

    @PatchMapping("/{id}/receipt-number")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntryResponseDto> updateReceiptNumber(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReceiptNumberRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        var command = requestMapper.toUpdateReceiptNumberCommand(id, request, currentUser);
        return ResponseEntity.ok(entryService.updateReceiptNumber(command));
    }

    @PatchMapping("/{id}/exchange-rate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntryResponseDto> updateExchangeRate(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExchangeRateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        var command = requestMapper.toUpdateExchangeRateCommand(id, request, currentUser);
        return ResponseEntity.ok(entryService.updateExchangeRate(command));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DELETE (access checked in service)
    // ═══════════════════════════════════════════════════════════════════════════

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteEntry(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        var command = requestMapper.toDeleteEntryCommand(id, currentUser);
        entryService.deleteEntry(command);
        return ResponseEntity.noContent().build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // USER CAPABILITIES (for frontend)
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/capabilities")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FinancialEntryService.UserCapabilities> getUserCapabilities(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(entryService.getUserCapabilities(currentUser));
    }
}