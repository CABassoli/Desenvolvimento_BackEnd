package com.ecommerce.integration.interfaces;

/**
 * Exception for email service operations.
 * Provides standardized error handling for email-related failures.
 */
public class EmailException extends Exception {
    
    private final String errorCode;
    private final boolean retryable;
    
    public EmailException(String message) {
        super(message);
        this.errorCode = "UNKNOWN";
        this.retryable = false;
    }
    
    public EmailException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = false;
    }
    
    public EmailException(String message, String errorCode, boolean retryable) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }
    
    public EmailException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "UNKNOWN";
        this.retryable = false;
    }
    
    public EmailException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.retryable = false;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public boolean isRetryable() {
        return retryable;
    }
}