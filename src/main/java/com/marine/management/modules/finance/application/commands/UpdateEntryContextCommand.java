package com.marine.management.modules.finance.application.commands;

import com.marine.management.modules.users.domain.User;

import java.util.UUID;

public record UpdateEntryContextCommand(
        UUID entryId,
        Long whoId,
        Long mainCategoryId,
        String recipient,
        String country,
        String city,
        String specificLocation,
        String vendor,
        User updater
) {}