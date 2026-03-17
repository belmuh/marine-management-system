package com.marine.management.modules.finance.presentation;

import com.marine.management.modules.finance.application.AttachmentService;
import com.marine.management.modules.finance.domain.enums.AttachmentType;
import com.marine.management.modules.finance.presentation.dto.AttachmentResponseDto;
import com.marine.management.modules.users.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/finance/entries")
public class EntryAttachmentController {

    private final AttachmentService attachmentService;

    public EntryAttachmentController(AttachmentService attachmentService){
        this.attachmentService = attachmentService;
    }

    @PostMapping("/{id}/attachments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttachmentResponseDto> addAttachment(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("attachmentType") AttachmentType attachmentType,
            @AuthenticationPrincipal User currentUser
    ) {
        var attachment = attachmentService.addAttachment(id, file, attachmentType, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(attachment);
    }

    @PostMapping("/{id}/attachments/bulk")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AttachmentResponseDto>> addAttachments(
            @PathVariable UUID id,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("attachmentType") AttachmentType attachmentType,
            @AuthenticationPrincipal User currentUser
    ) {
        var attachments = attachmentService.addAttachments(id, files, attachmentType, currentUser);
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

    /**
     * Returns a short-lived presigned R2 URL for direct browser download.
     * The client opens this URL directly — no bandwidth through the app server.
     * Response: { "url": "https://..." }
     */
    @GetMapping("/{id}/attachments/{attachmentId}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> getDownloadUrl(
            @PathVariable UUID id,
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal User currentUser
    ) {
        String presignedUrl = attachmentService.getDownloadUrl(id, attachmentId, currentUser);
        return ResponseEntity.ok(Map.of("url", presignedUrl));
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