package com.marine.management.modules.finance.presentation.dto.controller;

import java.time.LocalDate;
import java.util.List;

public record UpdateEntryMetadataRequest(
        String frequency,
        String priority,
        String tags
) {

}
