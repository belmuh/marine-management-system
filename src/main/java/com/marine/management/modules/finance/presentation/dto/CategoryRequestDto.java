package com.marine.management.modules.finance.presentation.dto;

import com.marine.management.modules.finance.domain.enums.RecordType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryRequestDto(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @NotNull
        RecordType categoryType,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        Integer displayOrder,

        boolean technical
) {

}