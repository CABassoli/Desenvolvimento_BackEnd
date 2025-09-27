package com.ecommerce.integration;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class StripeCardPaymentRequest {
    private UUID orderId;
    private UUID customerId;
    private BigDecimal amount;
    private Long amountCents; // Valor em centavos para Stripe
    private String cardToken;
    private String cardBrand;
    private String currency;
    
    public static StripeCardPaymentRequest from(UUID orderId, UUID customerId, 
                                              BigDecimal amount, String cardToken, String cardBrand) {
        return StripeCardPaymentRequest.builder()
            .orderId(orderId)
            .customerId(customerId)
            .amount(amount)
            .amountCents(amount.multiply(BigDecimal.valueOf(100)).longValue())
            .cardToken(cardToken)
            .cardBrand(cardBrand)
            .currency("brl")
            .build();
    }
}