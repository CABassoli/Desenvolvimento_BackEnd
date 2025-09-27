package com.ecommerce.integration;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class DeliveryPartnerWebhook {
    private String orderId;
    private String trackingCode;
    private String recipientName;
    private String recipientPhone;
    private String deliveryAddress;
    private String scheduledDate;
    private LocalDateTime timestamp;
    private String instructions;
    
    public static DeliveryPartnerWebhook create(String orderId, String recipientName, String deliveryAddress) {
        return DeliveryPartnerWebhook.builder()
            .orderId(orderId)
            .trackingCode("TR" + System.currentTimeMillis())
            .recipientName(recipientName)
            .deliveryAddress(deliveryAddress)
            .timestamp(LocalDateTime.now())
            .build();
    }
}