package com.ecommerce.integration;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class StripeBoletoRequest {
    private UUID orderId;
    private UUID customerId;
    private BigDecimal amount;
    private String customerName;
    private String customerEmail;
    private String customerDocument;
    
    public static StripeBoletoRequest from(UUID orderId, UUID customerId, 
                                         BigDecimal amount, String customerName, String customerEmail) {
        return StripeBoletoRequest.builder()
            .orderId(orderId)
            .customerId(customerId)
            .amount(amount)
            .customerName(customerName)
            .customerEmail(customerEmail)
            .build();
    }
}