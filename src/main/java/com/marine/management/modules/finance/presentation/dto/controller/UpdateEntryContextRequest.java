package com.marine.management.modules.finance.presentation.dto.controller;

import java.util.UUID;

public record UpdateEntryContextRequest(
        UUID whoId,
        UUID mainCategoryId,
        String recipient,
        String country,
        String city,
        String specificLocation,
        String vendor
) {
}
