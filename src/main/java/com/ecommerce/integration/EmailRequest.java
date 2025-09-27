package com.ecommerce.integration;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmailRequest {
    private String toEmail;
    private String subject;
    private String body;
    private boolean html;
    private String type; // ORDER_CONFIRMATION, STATUS_UPDATE, WELCOME, PASSWORD_RESET
    
    public static EmailRequest createOrderConfirmation(String toEmail, String customerName, String orderId, String total) {
        return EmailRequest.builder()
            .toEmail(toEmail)
            .subject("Confirmação do Pedido #" + orderId)
            .body("Pedido confirmado para " + customerName + " - Total: R$ " + total)
            .html(false)
            .type("ORDER_CONFIRMATION")
            .build();
    }
    
    public static EmailRequest createStatusUpdate(String toEmail, String orderId, String status) {
        return EmailRequest.builder()
            .toEmail(toEmail)
            .subject("Atualização do Pedido #" + orderId)
            .body("Status do pedido: " + status)
            .html(false)
            .type("STATUS_UPDATE")
            .build();
    }
}