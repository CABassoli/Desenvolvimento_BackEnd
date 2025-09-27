package com.ecommerce.integration.interfaces;

/**
 * Exception for payment service operations.
 * Provides standardized error handling for payment-related failures.
 */
public class PaymentException extends Exception {
    
    private final String errorCode;
    private final boolean retryable;
    private final String providerErrorCode;
    
    public PaymentException(String message) {
        super(message);
        this.errorCode = "UNKNOWN";
        this.retryable = false;
        this.providerErrorCode = null;
    }
    
    public PaymentException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = false;
        this.providerErrorCode = null;
    }
    
    public PaymentException(String message, String errorCode, boolean retryable) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = retryable;
        this.providerErrorCode = null;
    }
    
    public PaymentException(String message, String errorCode, String providerErrorCode) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = false;
        this.providerErrorCode = providerErrorCode;
    }
    
    public PaymentException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "UNKNOWN";
        this.retryable = false;
        this.providerErrorCode = null;
    }
    
    public PaymentException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.retryable = false;
        this.providerErrorCode = null;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public boolean isRetryable() {
        return retryable;
    }
    
    public String getProviderErrorCode() {
        return providerErrorCode;
    }
}