package com.marine.management.shared.config;

import com.marine.management.modules.finance.domain.exceptions.EntryValidationException;
import com.marine.management.modules.finance.domain.exceptions.UnauthorizedActionException;
import com.marine.management.shared.exceptions.*;
import com.marine.management.shared.presentation.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;


import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        logger.warn("Validation [ID: {}] failed at {}: {}", errorId, request.getRequestURI(), ex.getMessage());

        String errorMessage = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        "Validation failed",  // error
                        errorMessage,         // message
                        "VALIDATION_ERROR",   // code
                        errorId               // errorId
                ));
    }

    /**
     * JPA lifecycle validation failures (IllegalStateException from @PrePersist/@PreUpdate)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        logger.warn("Validation error [ID: {}] at {}: {}",
                errorId, request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "Validation Error",
                        ex.getMessage(),
                        "VALIDATION_ERROR",
                        errorId
                ));
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationFailed(
            AuthenticationFailedException ex,
            HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        logger.warn("Authentication failed [ID: {}] at {}: {}", errorId, request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                        "Authentication Failed",    // error
                        ex.getMessage(),            // message
                        "AUTHENTICATION_FAILED",    // code
                        errorId                     // errorId
                ));
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccess(
            UnauthorizedAccessException ex,
            HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        logger.warn("Unauthorized Access [ID: {}] at {}: {}", errorId, request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        "Unauthorized Access",      // error
                        ex.getMessage(),            // message
                        "UNAUTHORIZED_ACCESS",      // code
                        errorId                     // errorId
                ));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex,
            HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        logger.info("User not found [ID: {}] at {}: {}", errorId, request.getRequestURI(), ex.getMessage());

        if (request.getRequestURI().contains("/auth/")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(
                            "Authentication Failed",     // error
                            "Invalid credentials",       // message
                            "AUTHENTICATION_FAILED",     // code
                            errorId                      // errorId
                    ));
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        "Not Found",          // error
                        "User not found",     // message
                        "USER_NOT_FOUND",     // code
                        errorId               // errorId
                ));
    }

    @ExceptionHandler(UserRegistrationException.class)
    public ResponseEntity<ErrorResponse> handleUserRegistration(
            UserRegistrationException ex,
            HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        logger.warn("User registration failed [ID: {}] at {}: {}", errorId, request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        "Registration Failed",          // error
                        ex.getMessage(),                // message
                        "USER_REGISTRATION_ERROR",      // code
                        errorId                         // errorId
                ));
    }

    @ExceptionHandler(UserUpdateException.class)
    public ResponseEntity<ErrorResponse> handleUserUpdate(
            UserUpdateException ex,
            HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        logger.warn("User update failed [ID: {}] at {}: {}", errorId, request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        "Update Failed",           // error
                        ex.getMessage(),           // message
                        "USER_UPDATE_ERROR",       // code
                        errorId                    // errorId
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        logger.error("Unexpected error [ID: {}] at {}: {}", errorId, request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "Internal Server Error",            // error
                        "An unexpected error occurred",     // message
                        "INTERNAL_ERROR",                   // code
                        errorId                             // errorId
                ));
    }


    /**
     * Domain authorization exceptions
     */
    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAction(
            UnauthorizedActionException ex,
            HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        logger.warn("Unauthorized Action [ID: {}] at {}: {}",
                errorId, request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        "Unauthorized Action",
                        ex.getMessage(),
                        "UNAUTHORIZED_ACTION",
                        errorId
                ));
    }

    /**
     * Domain validation exceptions (field-level errors)
     */
    @ExceptionHandler(EntryValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleEntryValidation(
            EntryValidationException ex,
            HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        logger.warn("Entry Validation Failed [ID: {}] at {}: {}",
                errorId, request.getRequestURI(), ex.getMessage());

        return ResponseEntity.badRequest()
                .body(new ValidationErrorResponse(
                        "Validation Failed",
                        ex.getMessage(),
                        ex.getErrors().stream()
                                .collect(Collectors.toMap(
                                        EntryValidationException.ValidationError::field,
                                        EntryValidationException.ValidationError::message
                                )),
                        "ENTRY_VALIDATION_ERROR",
                        errorId
                ));
    }

    //  ValidationErrorResponse - Nested class (ErrorResponse'dan ayrı)
    public record ValidationErrorResponse(
            String error,
            String message,
            Map<String, String> fieldErrors,
            String code,
            String errorId
    ) {}


}