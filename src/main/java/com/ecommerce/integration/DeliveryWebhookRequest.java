package com.ecommerce.integration;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DeliveryWebhookRequest {
    private String orderId;
    private String status; // PROCESSANDO, ENVIADO, ENTREGUE, CANCELADO
    private String trackingCode;
    private String carrierName;
    private String location;
    private LocalDateTime timestamp;
    private String signature; // Para validação de segurança
    private String notes;
    
    // Status válidos para entrega
    public static final String STATUS_PREPARING = "PROCESSANDO";
    public static final String STATUS_SHIPPED = "ENVIADO";
    public static final String STATUS_DELIVERED = "ENTREGUE";
    public static final String STATUS_CANCELLED = "CANCELADO";
    
    public boolean isValidStatus() {
        return status != null && (
            status.equals(STATUS_PREPARING) ||
            status.equals(STATUS_SHIPPED) ||
            status.equals(STATUS_DELIVERED) ||
            status.equals(STATUS_CANCELLED)
        );
    }
}