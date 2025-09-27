package com.ecommerce.integration;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TwilioSmsRequest {
    private String phoneNumber;
    private String message;
    private String type; // ORDER_CONFIRMATION, STATUS_UPDATE, VERIFICATION
    
    public static TwilioSmsRequest createOrderConfirmation(String phoneNumber, String orderId, String customerName) {
        return TwilioSmsRequest.builder()
            .phoneNumber(phoneNumber)
            .message(String.format("Pedido %s confirmado para %s", orderId, customerName))
            .type("ORDER_CONFIRMATION")
            .build();
    }
    
    public static TwilioSmsRequest createStatusUpdate(String phoneNumber, String orderId, String status) {
        return TwilioSmsRequest.builder()
            .phoneNumber(phoneNumber)
            .message(String.format("Pedido %s: %s", orderId, status))
            .type("STATUS_UPDATE")
            .build();
    }
}