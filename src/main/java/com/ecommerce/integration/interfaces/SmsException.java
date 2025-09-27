package com.ecommerce.integration.interfaces;

/**
 * Exception for SMS service operations.
 * Provides standardized error handling for SMS-related failures.
 */
public class SmsException extends Exception {
    
    private final String errorCode;
    private final boolean retryable;
    
    public SmsException(String message) {
        super(message);
        this.errorCode = "UNKNOWN";
        this.retryable = false;
    }
    
    public SmsException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = false;
    }
    
    public SmsException(String message, String errorCode, boolean retryable) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }
    
    public SmsException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "UNKNOWN";
        this.retryable = false;
    }
    
    public SmsException(String message, String errorCode, Throwable cause) {
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