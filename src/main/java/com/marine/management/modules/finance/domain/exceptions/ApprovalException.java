package com.marine.management.modules.finance.domain.exceptions;

/**
 * Domain exception for approval workflow violations.
 */
public class ApprovalException extends RuntimeException {

  public ApprovalException(String message) {
    super(message);
  }

  public ApprovalException(String message, Throwable cause) {
    super(message, cause);
  }
}