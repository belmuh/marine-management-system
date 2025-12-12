
package com.marine.management.shared.exceptions;

import java.util.UUID;

public class AttachmentNotFoundException extends RuntimeException {

    public static AttachmentNotFoundException withId(UUID attachmentId) {
        return new AttachmentNotFoundException(
                String.format("Attachment with id '%s' not found", attachmentId)
        );
    }

    public static AttachmentNotFoundException withEntryAndAttachmentId(
            UUID entryId,
            UUID attachmentId
    ) {
        return new AttachmentNotFoundException(
                String.format(
                        "Attachment with id '%s' not found for entry '%s'",
                        attachmentId,
                        entryId
                )
        );
    }

    public static AttachmentNotFoundException withFileName(String fileName) {
        return new AttachmentNotFoundException(
                String.format("Attachment file '%s' not found in storage", fileName)
        );
    }

    private AttachmentNotFoundException(String message) {
        super(message);
    }

    private AttachmentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}