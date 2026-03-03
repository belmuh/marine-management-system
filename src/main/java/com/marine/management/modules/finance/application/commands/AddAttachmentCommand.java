package com.marine.management.modules.finance.application.commands;

import com.marine.management.modules.finance.domain.entities.FinancialEntryAttachment;
import com.marine.management.modules.users.domain.User;

import java.util.UUID;

public record AddAttachmentCommand(
        UUID entryId,
        FinancialEntryAttachment attachment,
        User updater
) {}
