package com.finance.exception;

/**
 * Kaynak bulunamadığında fırlatılır (404).
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
