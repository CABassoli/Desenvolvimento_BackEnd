package com.ecommerce.integration;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TwilioSmsResult {
    private boolean success;
    private String messageSid;
    private String status;
    private String message;
    private String errorMessage;
    
    public boolean isSuccess() {
        return success;
    }
    
    public boolean hasError() {
        return !success;
    }
}