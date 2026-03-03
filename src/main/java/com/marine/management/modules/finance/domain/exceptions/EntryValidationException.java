package com.marine.management.modules.finance.domain.exceptions;

import java.util.List;

/**
 * Domain exception for financial entry validation failures.
 * Contains field-level errors for detailed feedback.
 */
public class EntryValidationException extends RuntimeException {

  private final List<ValidationError> errors;

  public EntryValidationException(String message, List<ValidationError> errors) {
    super(message);
    this.errors = errors;
  }

  public List<ValidationError> getErrors() {
    return errors;
  }

  /**
   * Field-level validation error
   */
  public record ValidationError(String field, String message) {}
}