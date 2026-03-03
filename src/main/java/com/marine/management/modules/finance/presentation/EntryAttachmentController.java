package com.marine.management.modules.finance.presentation;

import com.marine.management.modules.finance.application.AttachmentService;
import com.marine.management.modules.finance.presentation.dto.AttachmentResponseDto;
import com.marine.management.modules.users.domain.User;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/finance/entries")
public class EntryAttachmentController {

    private final AttachmentService attachmentService;

    public EntryAttachmentController(AttachmentService attachmentService){
        this.attachmentService = attachmentService;
    }

    @PostMapping("/{id}/attachments")
    @PreAuthorize("isAuthenticated()")  // 🆕 Ekle
    public ResponseEntity<AttachmentResponseDto> addAttachment(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser
    ) {
        var attachment = attachmentService.addAttachment(id, file, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(attachment);
    }

    @PostMapping("/{id}/attachments/bulk")
    @PreAuthorize("isAuthenticated()")  // 🆕 Ekle
    public ResponseEntity<List<AttachmentResponseDto>> addAttachments(
            @PathVariable UUID id,
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal User currentUser
    ) {
        var attachments = attachmentService.addAttachments(id, files, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(attachments);
    }

    @GetMapping("/{id}/attachments")
    @PreAuthorize("isAuthenticated()")  // 🆕 Ekle
    public ResponseEntity<List<AttachmentResponseDto>> getAttachments(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser  // 🆕 Ekle - access control için
    ) {
        return ResponseEntity.ok(attachmentService.getAttachments(id, currentUser));
    }

    @GetMapping("/{id}/attachments/{attachmentId}/download")
    @PreAuthorize("isAuthenticated()")  // 🆕 Ekle
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable UUID id,
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal User currentUser  // 🆕 Ekle - access control için
    ) {
        return attachmentService.downloadAttachment(id, attachmentId, currentUser);
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    @PreAuthorize("isAuthenticated()")  // 🆕 Ekle
    public ResponseEntity<Void> removeAttachment(
            @PathVariable UUID id,
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal User currentUser
    ) {
        attachmentService.removeAttachment(id, attachmentId, currentUser);
        return ResponseEntity.noContent().build();
    }
}