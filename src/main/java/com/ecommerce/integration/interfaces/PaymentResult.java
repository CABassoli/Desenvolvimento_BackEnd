package com.ecommerce.integration.interfaces;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Result object for payment operations.
 * Based on Stripe integration patterns for unified payment response.
 */
public class PaymentResult {
    
    private final String paymentId;
    private final String externalId;
    private final PaymentStatus status;
    private final BigDecimal amount;
    private final String currency;
    private final String method;
    private final String details;
    private final String qrCode;
    private final String pixCode;
    private final String boletoUrl;
    private final String boletoBarcode;
    private final LocalDateTime expiresAt;
    private final boolean success;
    private final String errorMessage;
    
    private PaymentResult(Builder builder) {
        this.paymentId = builder.paymentId;
        this.externalId = builder.externalId;
        this.status = builder.status;
        this.amount = builder.amount;
        this.currency = builder.currency;
        this.method = builder.method;
        this.details = builder.details;
        this.qrCode = builder.qrCode;
        this.pixCode = builder.pixCode;
        this.boletoUrl = builder.boletoUrl;
        this.boletoBarcode = builder.boletoBarcode;
        this.expiresAt = builder.expiresAt;
        this.success = builder.success;
        this.errorMessage = builder.errorMessage;
    }
    
    // Getters
    public String getPaymentId() { return paymentId; }
    public String getExternalId() { return externalId; }
    public PaymentStatus getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getMethod() { return method; }
    public String getDetails() { return details; }
    public String getQrCode() { return qrCode; }
    public String getPixCode() { return pixCode; }
    public String getBoletoUrl() { return boletoUrl; }
    public String getBoletoBarcode() { return boletoBarcode; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
    
    public static class Builder {
        private String paymentId;
        private String externalId;
        private PaymentStatus status;
        private BigDecimal amount;
        private String currency = "BRL";
        private String method;
        private String details;
        private String qrCode;
        private String pixCode;
        private String boletoUrl;
        private String boletoBarcode;
        private LocalDateTime expiresAt;
        private boolean success;
        private String errorMessage;
        
        public Builder paymentId(String paymentId) { this.paymentId = paymentId; return this; }
        public Builder externalId(String externalId) { this.externalId = externalId; return this; }
        public Builder status(PaymentStatus status) { this.status = status; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder method(String method) { this.method = method; return this; }
        public Builder details(String details) { this.details = details; return this; }
        public Builder qrCode(String qrCode) { this.qrCode = qrCode; return this; }
        public Builder pixCode(String pixCode) { this.pixCode = pixCode; return this; }
        public Builder boletoUrl(String boletoUrl) { this.boletoUrl = boletoUrl; return this; }
        public Builder boletoBarcode(String boletoBarcode) { this.boletoBarcode = boletoBarcode; return this; }
        public Builder expiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; return this; }
        public Builder success(boolean success) { this.success = success; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        
        public PaymentResult build() {
            return new PaymentResult(this);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}