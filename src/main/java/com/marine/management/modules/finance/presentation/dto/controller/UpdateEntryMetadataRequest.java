package com.marine.management.modules.finance.presentation.dto.controller;

public record UpdateEntryMetadataRequest(
        String frequency,
        String priority,
        String tags
) {
}
