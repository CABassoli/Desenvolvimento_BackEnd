package com.ecommerce.integration;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WebhookResult {
    private boolean success;
    private String message;
    private String errorMessage;
    private int httpStatus;
    private String webhookId;
    
    public boolean isSuccess() {
        return success;
    }
    
    public boolean hasError() {
        return !success;
    }
}