package com.ecommerce.integration;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderUpdateWebhook {
    private String orderId;
    private String customerId;
    private String status;
    private String previousStatus;
    private LocalDateTime timestamp;
    private String message;
    private String trackingCode;
    
    public static OrderUpdateWebhook create(String orderId, String customerId, String status, String message) {
        return OrderUpdateWebhook.builder()
            .orderId(orderId)
            .customerId(customerId)
            .status(status)
            .timestamp(LocalDateTime.now())
            .message(message)
            .build();
    }
}