package com.ecommerce.integration;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class StripePixPaymentRequest {
    private UUID orderId;
    private UUID customerId;
    private BigDecimal amount;
    private Long amountCents;
    private String pixKey;
    private String currency;
    
    public static StripePixPaymentRequest from(UUID orderId, UUID customerId, BigDecimal amount) {
        return StripePixPaymentRequest.builder()
            .orderId(orderId)
            .customerId(customerId)
            .amount(amount)
            .amountCents(amount.multiply(BigDecimal.valueOf(100)).longValue())
            .currency("brl")
            .build();
    }
}