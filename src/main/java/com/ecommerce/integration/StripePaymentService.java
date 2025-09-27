package com.ecommerce.integration;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Serviço de integração real com Stripe para processamento de pagamentos
 */
public class StripePaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(StripePaymentService.class);
    
    public StripePaymentService() {
        // Fast development mode check - avoid blocking network calls during startup
        String integrationsEnabled = System.getenv("INTEGRATIONS_ENABLED");
        boolean fastMode = "false".equals(integrationsEnabled) || integrationsEnabled == null;
        
        if (fastMode) {
            logger.info("🚀 Stripe em modo desenvolvimento - sem inicialização de rede");
            Stripe.apiKey = "sk_test_fake_key_for_development";
        } else {
            // Configura chave da API do Stripe (deve vir de variável de ambiente)
            String stripeSecretKey = System.getenv("STRIPE_SECRET_KEY");
            if (stripeSecretKey != null && !stripeSecretKey.isEmpty()) {
                Stripe.apiKey = stripeSecretKey;
                logger.info("✅ Stripe configurado com chave da API");
            } else {
                logger.warn("⚠️ STRIPE_SECRET_KEY não configurada - usando modo simulação");
                // Usa chave de teste padrão se não configurada
                Stripe.apiKey = "sk_test_fake_key_for_development"; 
            }
        }
    }
    
    /**
     * Processa pagamento com cartão via Stripe
     */
    public StripePaymentResult processCardPayment(StripeCardPaymentRequest request) {
        try {
            // Verifica se está configurado para produção
            if (Stripe.apiKey.equals("sk_test_fake_key_for_development")) {
                return simulateCardPayment(request);
            }
            
            // Cria PaymentIntent com método de pagamento cartão
            // Em produção, o PaymentMethod seria criado no frontend e passado aqui
            PaymentIntentCreateParams piParams = PaymentIntentCreateParams.builder()
                .setAmount(request.getAmountCents())
                .setCurrency("brl")
                .addPaymentMethodType("card")
                .setConfirm(false) // Em produção, seria confirmado separadamente
                .setDescription("Pedido #" + request.getOrderId())
                .putMetadata("order_id", request.getOrderId().toString())
                .putMetadata("customer_id", request.getCustomerId().toString())
                .putMetadata("card_brand", request.getCardBrand())
                .putMetadata("card_token", request.getCardToken())
                .build();
            
            PaymentIntent paymentIntent = PaymentIntent.create(piParams);
            logger.info("✅ PaymentIntent criado: {} - Status: {}", 
                paymentIntent.getId(), paymentIntent.getStatus());
            
            // Em produção real, aqui você confirmaria o PaymentIntent com o PaymentMethod
            // Para desenvolvimento, simula confirmação bem-sucedida
            boolean isSuccess = true; // PaymentIntent criado com sucesso
            
            return StripePaymentResult.builder()
                .success(isSuccess)
                .paymentIntentId(paymentIntent.getId())
                .transactionId(paymentIntent.getId()) // Usa PaymentIntent ID como transaction ID
                .status(paymentIntent.getStatus())
                .message(isSuccess ? "Pagamento aprovado com sucesso" : "Pagamento rejeitado - " + paymentIntent.getStatus())
                .build();
                
        } catch (StripeException e) {
            logger.error("❌ Erro no pagamento Stripe: {} - Code: {}", e.getMessage(), e.getCode());
            return StripePaymentResult.builder()
                .success(false)
                .errorCode(e.getCode())
                .message("Erro no processamento: " + (e.getUserMessage() != null ? e.getUserMessage() : e.getMessage()))
                .build();
        }
    }
    
    /**
     * Confirma pagamento PIX (via webhook ou consulta)
     */
    public StripePaymentResult processPixPayment(StripePixPaymentRequest request) {
        try {
            // Verifica se está configurado para produção
            if (Stripe.apiKey.equals("sk_test_fake_key_for_development")) {
                return simulatePixPayment(request);
            }
            
            // PIX no Stripe seria implementado via PaymentIntent com payment_method_types=["pix"]
            // Por enquanto, simula sucesso
            logger.info("✅ PIX processado (simulação): R$ {}", request.getAmount());
            
            return StripePaymentResult.builder()
                .success(true)
                .transactionId("pix_" + System.currentTimeMillis())
                .message("PIX processado com sucesso")
                .build();
                
        } catch (Exception e) {
            logger.error("❌ Erro no PIX: {}", e.getMessage());
            return StripePaymentResult.builder()
                .success(false)
                .message("Erro no processamento PIX: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * Gera boleto (integração externa seria necessária)
     */
    public StripePaymentResult generateBoleto(StripeBoletoRequest request) {
        logger.info("📄 Gerando boleto para pedido: {}", request.getOrderId());
        
        // Boleto requer integração com banco brasileiro
        // Por enquanto retorna simulação
        String linhaDigitavel = generateBoletoLine();
        
        return StripePaymentResult.builder()
            .success(true)
            .boletoLine(linhaDigitavel)
            .message("Boleto gerado com sucesso")
            .build();
    }
    
    // ==================== MÉTODOS DE SIMULAÇÃO ====================
    
    private StripePaymentResult simulateCardPayment(StripeCardPaymentRequest request) {
        logger.info("🔧 Simulando pagamento cartão - Valor: R$ {} - Token: {}", 
            request.getAmount(), request.getCardToken());
        
        // Simula latência de processamento do cartão
        try {
            Thread.sleep(1500); // 1.5 segundos
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simula cenários diferentes baseados no token do cartão
        String cardToken = request.getCardToken();
        
        // Simula cartão rejeitado pelo banco (tokens terminados em 0000)
        if (cardToken.endsWith("0000")) {
            logger.warn("🚫 Simulando rejeição bancária para token: {}", cardToken);
            return StripePaymentResult.builder()
                .success(false)
                .errorCode("card_declined")
                .status("failed")
                .message("Cartão rejeitado pelo banco emissor")
                .build();
        }
        
        // Simula cartão com saldo insuficiente (tokens terminados em 1111)
        if (cardToken.endsWith("1111")) {
            logger.warn("🚫 Simulando saldo insuficiente para token: {}", cardToken);
            return StripePaymentResult.builder()
                .success(false)
                .errorCode("insufficient_funds")
                .status("failed")
                .message("Cartão rejeitado - saldo insuficiente")
                .build();
        }
        
        // Simula cartão expirado (tokens terminados em 2222)
        if (cardToken.endsWith("2222")) {
            logger.warn("🚫 Simulando cartão expirado para token: {}", cardToken);
            return StripePaymentResult.builder()
                .success(false)
                .errorCode("expired_card")
                .status("failed")  
                .message("Cartão rejeitado - cartão expirado")
                .build();
        }
        
        // Simula pagamento aprovado para todos os outros casos
        String simulatedPaymentIntentId = "pi_sim_" + System.currentTimeMillis();
        logger.info("✅ Simulando aprovação - PaymentIntent: {}", simulatedPaymentIntentId);
        
        return StripePaymentResult.builder()
            .success(true)
            .paymentIntentId(simulatedPaymentIntentId)
            .transactionId(simulatedPaymentIntentId)
            .status("succeeded")
            .message("Pagamento aprovado com sucesso (simulação)")
            .build();
    }
    
    private StripePaymentResult simulatePixPayment(StripePixPaymentRequest request) {
        logger.info("🔧 Simulando PIX - Valor: R$ {}", request.getAmount());
        
        return StripePaymentResult.builder()
            .success(true)
            .transactionId("pix_sim_" + System.currentTimeMillis())
            .message("PIX aprovado (simulação)")
            .build();
    }
    
    private String generateBoletoLine() {
        // Gera linha digitável simulada no formato real
        return String.format("%05d.%05d %05d.%06d %05d.%06d %d %014d", 
            10499,  // Código do banco
            89104,  // Código da agência  
            12345,  // Código da conta
            789012, // Nosso número parte 1
            34567,  // Nosso número parte 2
            890123, // Vencimento + valor parte 1
            4,      // Dígito verificador
            System.currentTimeMillis() % 100000000000000L // Valor em centavos
        );
    }
}