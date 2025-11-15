package com.marine.management.shared.config;

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

import java.util.UUID;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request){
        String errorId = UUID.randomUUID().toString();
        logger.warn("Validation [ID: {}] failed at {}: {}", errorId, request.getRequestURI(), ex.getMessage());
        String errorMessage = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        "Validation failed",
                        errorMessage,
                        "VALIDATION_ERROR",
                        errorId));
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationFailed(AuthenticationFailedException ex, HttpServletRequest request){
        String errorId = UUID.randomUUID().toString();
        logger.warn("Authentication failed [ID: {}] at {}: {}", errorId, request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                        ex.getMessage(),
                        "AUTHENTICATION_FAILED",
                        errorId));
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccess(UnauthorizedAccessException ex, HttpServletRequest request){
        String errorId = UUID.randomUUID().toString();
        logger.warn("Unauthorized Access [ID: {}] at {}: {}", errorId, request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        ex.getMessage(),
                        "UNAUTHORIZED_ACCESS",
                        errorId));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex, HttpServletRequest request){
        String errorId = UUID.randomUUID().toString();
        logger.info("User not found [ID: {}] at {}: {}", errorId, request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        ex.getMessage(),
                        "USER_NOT_FOUND",
                        errorId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request){
        String errorId = UUID.randomUUID().toString();
        logger.error("Unexpected error [ID: {}] at {}: {}", errorId, request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "An unexpected error occurred",
                        "INTERNAL_ERROR",
                        errorId ));
    }

    // İleride ekleyebilirsin:
    @ExceptionHandler(UserRegistrationException.class)
    public ResponseEntity<ErrorResponse> handleUserRegistration(UserRegistrationException ex, HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        logger.warn("User registration failed [ID: {}] at {}: {}", errorId, request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage(), "USER_REGISTRATION_ERROR", errorId));
    }

    @ExceptionHandler(UserUpdateException.class)
    public ResponseEntity<ErrorResponse> handleUserUpdate(UserUpdateException ex, HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        logger.warn("User update failed [ID: {}] at {}: {}", errorId, request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage(), "USER_UPDATE_ERROR", errorId));
    }
}
