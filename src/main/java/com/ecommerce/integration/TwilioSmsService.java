package com.ecommerce.integration;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serviço de integração real com Twilio para envio de SMS
 */
public class TwilioSmsService {
    
    private static final Logger logger = LoggerFactory.getLogger(TwilioSmsService.class);
    
    private final String accountSid;
    private final String authToken;
    private final String twilioPhoneNumber;
    private final boolean isConfigured;
    private boolean twilioInitialized = false;
    
    public TwilioSmsService() {
        // Fast development mode check - avoid blocking network calls during startup
        String integrationsEnabled = System.getenv("INTEGRATIONS_ENABLED");
        boolean fastMode = "false".equals(integrationsEnabled) || integrationsEnabled == null;
        
        this.accountSid = System.getenv("TWILIO_ACCOUNT_SID");
        this.authToken = System.getenv("TWILIO_AUTH_TOKEN");
        this.twilioPhoneNumber = System.getenv("TWILIO_PHONE_NUMBER");
        
        this.isConfigured = !fastMode && (accountSid != null && !accountSid.isEmpty() &&
                           authToken != null && !authToken.isEmpty() &&
                           twilioPhoneNumber != null && !twilioPhoneNumber.isEmpty());
        
        if (fastMode) {
            logger.info("🚀 Twilio em modo desenvolvimento - sem inicialização de rede");
        } else if (isConfigured) {
            // Defer Twilio.init() to first actual use to avoid blocking startup
            logger.info("⚙️ Twilio configurado - inicialização postergada");
        } else {
            logger.warn("⚠️ Twilio não configurado - usando modo simulação");
            logger.info("📝 Configure: TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_PHONE_NUMBER");
        }
    }
    
    /**
     * Envia SMS de notificação
     */
    public TwilioSmsResult sendSms(TwilioSmsRequest request) {
        try {
            if (!isConfigured) {
                return simulateSms(request);
            }
            
            // Initialize Twilio only when actually needed
            initializeTwilioIfNeeded();
            
            // Valida número de telefone brasileiro
            String formattedPhone = formatBrazilianPhone(request.getPhoneNumber());
            if (formattedPhone == null) {
                return TwilioSmsResult.builder()
                    .success(false)
                    .errorMessage("Número de telefone inválido")
                    .build();
            }
            
            // Envia SMS via Twilio
            Message message = Message.creator(
                new PhoneNumber(formattedPhone),
                new PhoneNumber(twilioPhoneNumber),
                request.getMessage()
            ).create();
            
            logger.info("✅ SMS enviado: {} -> {}", message.getSid(), formattedPhone);
            
            return TwilioSmsResult.builder()
                .success(true)
                .messageSid(message.getSid())
                .status(message.getStatus().toString())
                .message("SMS enviado com sucesso")
                .build();
                
        } catch (Exception e) {
            logger.error("❌ Erro ao enviar SMS: {}", e.getMessage());
            return TwilioSmsResult.builder()
                .success(false)
                .errorMessage("Erro no envio: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * Inicializa Twilio apenas quando necessário (não no construtor)
     */
    private void initializeTwilioIfNeeded() {
        if (isConfigured && !twilioInitialized) {
            Twilio.init(accountSid, authToken);
            twilioInitialized = true;
            logger.info("✅ Twilio inicializado on-demand");
        }
    }
    
    /**
     * Envia SMS de confirmação de pedido
     */
    public TwilioSmsResult sendOrderConfirmation(String phoneNumber, String orderId, String customerName) {
        String message = String.format(
            "🛒 Olá %s! Seu pedido #%s foi confirmado e está sendo processado. " +
            "Você receberá atualizações sobre o status da entrega.",
            customerName, orderId
        );
        
        TwilioSmsRequest request = TwilioSmsRequest.builder()
            .phoneNumber(phoneNumber)
            .message(message)
            .type("ORDER_CONFIRMATION")
            .build();
            
        return sendSms(request);
    }
    
    /**
     * Envia SMS de atualização de status
     */
    public TwilioSmsResult sendStatusUpdate(String phoneNumber, String orderId, String status) {
        String statusMessage = getStatusMessage(status);
        String message = String.format(
            "📦 Atualização do pedido #%s: %s. " +
            "Acesse nossa plataforma para mais detalhes.",
            orderId, statusMessage
        );
        
        TwilioSmsRequest request = TwilioSmsRequest.builder()
            .phoneNumber(phoneNumber)
            .message(message)
            .type("STATUS_UPDATE")
            .build();
            
        return sendSms(request);
    }
    
    /**
     * Envia SMS de código de verificação
     */
    public TwilioSmsResult sendVerificationCode(String phoneNumber, String code) {
        String message = String.format(
            "🔐 Seu código de verificação: %s\n" +
            "Este código expira em 10 minutos. Não compartilhe com ninguém.",
            code
        );
        
        TwilioSmsRequest request = TwilioSmsRequest.builder()
            .phoneNumber(phoneNumber)
            .message(message)
            .type("VERIFICATION")
            .build();
            
        return sendSms(request);
    }
    
    // ==================== MÉTODOS AUXILIARES ====================
    
    private String formatBrazilianPhone(String phone) {
        if (phone == null) return null;
        
        // Remove caracteres não numéricos
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        
        // Valida formato brasileiro
        if (cleanPhone.length() == 11 && cleanPhone.startsWith("55")) {
            // Já tem código do país
            return "+" + cleanPhone;
        } else if (cleanPhone.length() == 11) {
            // Adiciona código do país
            return "+55" + cleanPhone;
        } else if (cleanPhone.length() == 10) {
            // Celular sem 9 inicial
            if (cleanPhone.charAt(2) != '9') {
                cleanPhone = cleanPhone.substring(0, 2) + "9" + cleanPhone.substring(2);
            }
            return "+55" + cleanPhone;
        }
        
        return null; // Formato inválido
    }
    
    private String getStatusMessage(String status) {
        switch (status.toUpperCase()) {
            case "PROCESSANDO":
                return "Pedido confirmado e sendo preparado";
            case "ENVIADO":
                return "Produto enviado e a caminho";
            case "ENTREGUE":
                return "Produto entregue com sucesso";
            case "CANCELADO":
                return "Pedido cancelado";
            default:
                return "Status atualizado";
        }
    }
    
    private TwilioSmsResult simulateSms(TwilioSmsRequest request) {
        logger.info("🔧 Simulando SMS: {} -> {}", 
                   request.getPhoneNumber(), request.getMessage().substring(0, Math.min(50, request.getMessage().length())) + "...");
        
        return TwilioSmsResult.builder()
            .success(true)
            .messageSid("SMS_SIM_" + System.currentTimeMillis())
            .status("delivered")
            .message("SMS simulado com sucesso")
            .build();
    }
}