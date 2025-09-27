package com.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Serviço para gerenciar webhooks de entrega e integrações externas
 */
public class WebhookService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public WebhookService() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Processa webhook de status de entrega
     */
    public WebhookResult processDeliveryWebhook(DeliveryWebhookRequest request) {
        try {
            logger.info("📦 Processando webhook de entrega: {} -> {}", 
                       request.getOrderId(), request.getStatus());
            
            // Valida dados obrigatórios
            if (request.getOrderId() == null || request.getStatus() == null) {
                return WebhookResult.builder()
                    .success(false)
                    .errorMessage("OrderId e Status são obrigatórios")
                    .build();
            }
            
            // Valida assinatura se configurada
            if (!validateWebhookSignature(request)) {
                return WebhookResult.builder()
                    .success(false)
                    .errorMessage("Assinatura do webhook inválida")
                    .build();
            }
            
            // Processa atualização
            return processStatusUpdate(request);
            
        } catch (Exception e) {
            logger.error("❌ Erro ao processar webhook: {}", e.getMessage());
            return WebhookResult.builder()
                .success(false)
                .errorMessage("Erro interno: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * Envia webhook para sistemas externos
     */
    public WebhookResult sendWebhook(String url, Object payload, String secret) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            
            Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(jsonPayload, MediaType.get("application/json")));
            
            // Adiciona cabeçalhos de segurança
            if (secret != null && !secret.isEmpty()) {
                String signature = generateWebhookSignature(jsonPayload, secret);
                requestBuilder.header("X-Webhook-Signature", signature);
            }
            
            requestBuilder.header("User-Agent", "ECommerce-Webhook/1.0");
            
            try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
                if (response.isSuccessful()) {
                    logger.info("✅ Webhook enviado: {} -> {}", url, response.code());
                    return WebhookResult.builder()
                        .success(true)
                        .httpStatus(response.code())
                        .message("Webhook enviado com sucesso")
                        .build();
                } else {
                    logger.warn("⚠️ Webhook falhou: {} -> {}", url, response.code());
                    return WebhookResult.builder()
                        .success(false)
                        .httpStatus(response.code())
                        .errorMessage("HTTP " + response.code() + ": " + response.message())
                        .build();
                }
            }
            
        } catch (IOException e) {
            logger.error("❌ Erro de rede no webhook: {}", e.getMessage());
            return WebhookResult.builder()
                .success(false)
                .errorMessage("Erro de rede: " + e.getMessage())
                .build();
        } catch (Exception e) {
            logger.error("❌ Erro ao enviar webhook: {}", e.getMessage());
            return WebhookResult.builder()
                .success(false)
                .errorMessage("Erro interno: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * Registra webhook de notificação de pedido
     */
    public WebhookResult notifyOrderUpdate(String webhookUrl, OrderUpdateWebhook orderUpdate) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            logger.info("🔧 Simulando notificação de pedido: {}", orderUpdate.getOrderId());
            return WebhookResult.builder()
                .success(true)
                .message("Notificação simulada (webhook não configurado)")
                .build();
        }
        
        return sendWebhook(webhookUrl, orderUpdate, System.getenv("WEBHOOK_SECRET"));
    }
    
    /**
     * Webhook para sistemas de entrega (Correios, transportadoras)
     */
    public WebhookResult notifyDeliveryPartner(String partnerUrl, DeliveryPartnerWebhook delivery) {
        if (partnerUrl == null || partnerUrl.isEmpty()) {
            logger.info("🔧 Simulando notificação para transportadora: {}", delivery.getTrackingCode());
            return WebhookResult.builder()
                .success(true)
                .message("Notificação simulada (parceiro não configurado)")
                .build();
        }
        
        return sendWebhook(partnerUrl, delivery, System.getenv("DELIVERY_WEBHOOK_SECRET"));
    }
    
    // ==================== MÉTODOS AUXILIARES ====================
    
    private boolean validateWebhookSignature(DeliveryWebhookRequest request) {
        String expectedSignature = request.getSignature();
        String webhookSecret = System.getenv("WEBHOOK_SECRET");
        
        if (webhookSecret == null || expectedSignature == null) {
            // Se não há configuração de assinatura, aceita o webhook
            return true;
        }
        
        try {
            String payload = objectMapper.writeValueAsString(request);
            String calculatedSignature = generateWebhookSignature(payload, webhookSecret);
            return calculatedSignature.equals(expectedSignature);
        } catch (Exception e) {
            logger.error("Erro ao validar assinatura: {}", e.getMessage());
            return false;
        }
    }
    
    private String generateWebhookSignature(String payload, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                secret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] digest = mac.doFinal(payload.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return "sha256=" + sb.toString();
        } catch (Exception e) {
            logger.error("Erro ao gerar assinatura: {}", e.getMessage());
            return "";
        }
    }
    
    private WebhookResult processStatusUpdate(DeliveryWebhookRequest request) {
        logger.info("🔄 Atualizando status do pedido: {} -> {}", 
                   request.getOrderId(), request.getStatus());
        
        // Aqui seria feita a integração com o PedidoService para atualizar o status
        // Por enquanto, simula sucesso
        
        return WebhookResult.builder()
            .success(true)
            .message("Status atualizado com sucesso")
            .build();
    }
}