package com.marine.management.modules.finance.presentation.dto.controller;

public record UpdateEntryContextRequest(
        Long whoId,
        Long mainCategoryId,
        String recipient,
        String country,
        String city,
        String specificLocation,
        String vendor
) {
}
