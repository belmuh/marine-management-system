package com.marine.management.modules.finance.presentation.dto;

import com.marine.management.modules.finance.domain.entity.FinancialEntryAttachment;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AttachmentResponseDto(
        UUID id,
        String fileName,
        String originalFileName,
        String filePath,
        Long fileSize,
        String readableFileSize,
        String contentType,
        String fileExtension,
        boolean isImage,
        boolean isPdf,
        boolean isDocument,
        UploaderInfo uploadedBy,
        LocalDateTime uploadedAt,
        UUID entryId
) {

    public static AttachmentResponseDto from(FinancialEntryAttachment attachment) {
        if (attachment == null) {
            return null;
        }

        return new AttachmentResponseDto(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getOriginalFileName(),
                attachment.getFilePath(),
                attachment.getFileSize(),
                attachment.getReadableFileSize(),
                attachment.getContentType(),
                attachment.getFileExtension(),
                attachment.isImage(),
                attachment.isPdf(),
                attachment.isDocument(),
                attachment.getUploadedBy() != null ?
                        UploaderInfo.from(attachment.getUploadedBy()) : null,
                attachment.getUploadedAt(),
                attachment.getEntry() != null ? attachment.getEntry().getId() : null
        );
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UploaderInfo(
            UUID id,
            String username,
            String email,
            String fullName
    ) {
        public static UploaderInfo from(com.marine.management.modules.users.domain.User user) {
            if (user == null) return null;

            return new UploaderInfo(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getFullName()
            );
        }
    }
}