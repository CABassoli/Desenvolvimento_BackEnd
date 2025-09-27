package com.ecommerce.integration.interfaces;

/**
 * Payment status enumeration.
 * Standardized across all payment providers.
 */
public enum PaymentStatus {
    PENDING("pending"),
    PROCESSING("processing"),
    SUCCEEDED("succeeded"),
    FAILED("failed"),
    CANCELED("canceled"),
    REQUIRES_ACTION("requires_action"),
    REQUIRES_PAYMENT_METHOD("requires_payment_method"),
    REQUIRES_CONFIRMATION("requires_confirmation"),
    REQUIRES_CAPTURE("requires_capture"),
    EXPIRED("expired");
    
    private final String value;
    
    PaymentStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static PaymentStatus fromString(String value) {
        for (PaymentStatus status : PaymentStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        return PENDING; // Default fallback
    }
    
    @Override
    public String toString() {
        return value;
    }
}