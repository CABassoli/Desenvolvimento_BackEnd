package com.ecommerce.integration;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StripePaymentResult {
    private boolean success;
    private String paymentIntentId;
    private String transactionId;
    private String status;
    private String message;
    private String errorCode;
    private String boletoLine;
    private String pixQrCode;
    
    public boolean isSuccess() {
        return success;
    }
    
    public boolean hasError() {
        return !success;
    }
}