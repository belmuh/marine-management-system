package com.marine.management.modules.finance.domain.exceptions;

public class UnauthorizedActionException extends RuntimeException {

  public UnauthorizedActionException(String message) {
    super(message);
  }

  public UnauthorizedActionException(String message, Throwable cause) {
    super(message, cause);
  }
}