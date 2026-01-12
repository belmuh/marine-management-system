package com.marine.management.modules.finance.application.commands;

import com.marine.management.modules.users.domain.User;

import java.util.UUID;

public record DeleteEntryCommand(
        UUID entryId,
        User user
) {}
