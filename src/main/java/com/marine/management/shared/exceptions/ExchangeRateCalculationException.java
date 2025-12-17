package com.marine.management.shared.exceptions;

public class ExchangeRateCalculationException extends RuntimeException {

    public ExchangeRateCalculationException(String message) {
        super(message);
    }

    public ExchangeRateCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
