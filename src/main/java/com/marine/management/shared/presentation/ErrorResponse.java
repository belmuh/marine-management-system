package com.marine.management.shared.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;


@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String message,
        String detail,
        String errorCode,
        String referenceId,
        LocalDateTime timestamp
) {
    public ErrorResponse(String message, String errorCode, String errorId){
        this(message, null, errorCode, errorId, LocalDateTime.now());
    }

    public ErrorResponse(String message, String detail, String errorCode, String errorId){
        this(message, detail, errorCode, errorId, LocalDateTime.now());
    }
}
