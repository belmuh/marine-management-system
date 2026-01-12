package com.marine.management.modules.finance.application.commands;

import com.marine.management.modules.users.domain.User;

import java.util.UUID;

public record RemoveAttachmentCommand(
        UUID entryId,
        UUID attachmentId,
        User updater
) {}
