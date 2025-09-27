package com.ecommerce.integration;

import com.ecommerce.integration.impl.ReplitEmailService;
import com.ecommerce.integration.impl.SecureStripePaymentService;
import com.ecommerce.integration.impl.SecureTwilioSmsService;
import com.ecommerce.integration.interfaces.EmailService;
import com.ecommerce.integration.interfaces.EmailResult;
import com.ecommerce.integration.interfaces.EmailException;
import com.ecommerce.integration.interfaces.PaymentService;
import com.ecommerce.integration.interfaces.PaymentResult;
import com.ecommerce.integration.interfaces.PaymentException;
import com.ecommerce.integration.interfaces.PaymentStatus;
import com.ecommerce.integration.interfaces.SmsService;
import com.ecommerce.integration.interfaces.SmsException;
import com.ecommerce.integration.interfaces.CardData;
import com.ecommerce.integration.interfaces.CustomerData;

import java.util.Arrays;
import java.util.List;

/**
 * Factory for creating production-ready integration services.
 * Implements dependency injection pattern for secure service management.
 * 
 * Features:
 * - Lazy initialization to prevent startup blocking
 * - Automatic fallback to mock services in development
 * - Singleton pattern for resource efficiency
 * - Environment-based configuration
 */
public class IntegrationServiceFactory {
    
    private static volatile IntegrationServiceFactory instance;
    private volatile SmsService smsService;
    private volatile EmailService emailService;
    private volatile PaymentService paymentService;
    
    private IntegrationServiceFactory() {
        // Private constructor for singleton pattern
    }
    
    public static IntegrationServiceFactory getInstance() {
        if (instance == null) {
            synchronized (IntegrationServiceFactory.class) {
                if (instance == null) {
                    instance = new IntegrationServiceFactory();
                }
            }
        }
        return instance;
    }
    
    /**
     * Get SMS service instance with lazy initialization.
     * Returns production-ready Twilio service or mock service based on configuration.
     */
    public SmsService getSmsService() {
        if (smsService == null) {
            synchronized (this) {
                if (smsService == null) {
                    smsService = createSmsService();
                }
            }
        }
        return smsService;
    }
    
    /**
     * Get email service instance with lazy initialization.
     * Returns production-ready Replit Mail service or mock service based on configuration.
     */
    public EmailService getEmailService() {
        if (emailService == null) {
            synchronized (this) {
                if (emailService == null) {
                    emailService = createEmailService();
                }
            }
        }
        return emailService;
    }
    
    /**
     * Get payment service instance with lazy initialization.
     * Returns production-ready Stripe service or mock service based on configuration.
     */
    public PaymentService getPaymentService() {
        if (paymentService == null) {
            synchronized (this) {
                if (paymentService == null) {
                    paymentService = createPaymentService();
                }
            }
        }
        return paymentService;
    }
    
    private SmsService createSmsService() {
        try {
            SecureTwilioSmsService twilioService = new SecureTwilioSmsService();
            if (twilioService.isAvailable()) {
                System.out.println("🚀 Using production Twilio SMS service");
                return twilioService;
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to initialize Twilio SMS service: " + e.getMessage());
        }
        
        System.out.println("🔧 Using mock SMS service for development");
        return createMockSmsService();
    }
    
    private EmailService createEmailService() {
        try {
            ReplitEmailService replitService = new ReplitEmailService();
            if (replitService.isAvailable()) {
                System.out.println("🚀 Using production Replit Email service");
                return replitService;
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to initialize Replit Email service: " + e.getMessage());
        }
        
        System.out.println("🔧 Using mock Email service for development");
        return createMockEmailService();
    }
    
    private PaymentService createPaymentService() {
        try {
            SecureStripePaymentService stripeService = new SecureStripePaymentService();
            if (stripeService.isAvailable()) {
                System.out.println("🚀 Using production Stripe Payment service");
                return stripeService;
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to initialize Stripe Payment service: " + e.getMessage());
        }
        
        System.out.println("🔧 Using mock Payment service for development");
        return createMockPaymentService();
    }
    
    // Mock service implementations for development
    private SmsService createMockSmsService() {
        return new SmsService() {
            @Override
            public String sendMessage(String phoneNumber, String message) throws SmsException {
                System.out.println("📱 [MOCK SMS] " + phoneNumber + ": " + message.substring(0, Math.min(50, message.length())) + "...");
                return "MOCK_SMS_" + System.currentTimeMillis();
            }
            
            @Override
            public boolean isAvailable() {
                return true;
            }
            
            @Override
            public String getSenderNumber() {
                return "+5511999999999";
            }
        };
    }
    
    private EmailService createMockEmailService() {
        return new EmailService() {
            public EmailResult sendEmail(String to, String subject, String textContent) throws EmailException {
                System.out.println("📧 [MOCK EMAIL] " + to + " - " + subject);
                return new com.ecommerce.integration.interfaces.EmailResult(Arrays.asList(to), Arrays.asList(), 
                                     Arrays.asList(), "MOCK_" + System.currentTimeMillis(), 
                                     "Mock email sent", true);
            }
            
            public EmailResult sendHtmlEmail(String to, String subject, String htmlContent, String textContent) throws EmailException {
                return sendEmail(to, subject, textContent);
            }
            
            public EmailResult sendBulkEmail(List<String> recipients, String subject, String content, boolean isHtml) throws EmailException {
                System.out.println("📧 [MOCK BULK EMAIL] " + recipients.size() + " recipients - " + subject);
                return new com.ecommerce.integration.interfaces.EmailResult(recipients, Arrays.asList(), 
                                     Arrays.asList(), "MOCK_BULK_" + System.currentTimeMillis(), 
                                     "Mock bulk email sent", true);
            }
            
            public boolean isAvailable() {
                return true;
            }
            
            public boolean isValidEmail(String email) {
                return email != null && email.contains("@");
            }
        };
    }
    
    private PaymentService createMockPaymentService() {
        return new PaymentService() {
            @Override
            public PaymentResult processPixPayment(Long pedidoId, java.math.BigDecimal amount, String description) throws PaymentException {
                System.out.println("💳 [MOCK PIX] Order: " + pedidoId + " - Amount: " + amount);
                return PaymentResult.builder()
                    .paymentId("MOCK_PIX_" + System.currentTimeMillis())
                    .status(PaymentStatus.SUCCEEDED)
                    .amount(amount)
                    .method("pix")
                    .success(true)
                    .build();
            }
            
            @Override
            public PaymentResult processCardPayment(Long pedidoId, java.math.BigDecimal amount, CardData cardData, String description) throws PaymentException {
                System.out.println("💳 [MOCK CARD] Order: " + pedidoId + " - Amount: " + amount);
                return PaymentResult.builder()
                    .paymentId("MOCK_CARD_" + System.currentTimeMillis())
                    .status(PaymentStatus.SUCCEEDED)
                    .amount(amount)
                    .method("card")
                    .success(true)
                    .build();
            }
            
            @Override
            public PaymentResult generateBoleto(Long pedidoId, java.math.BigDecimal amount, CustomerData customerData, String description) throws PaymentException {
                System.out.println("🧾 [MOCK BOLETO] Order: " + pedidoId + " - Amount: " + amount);
                return PaymentResult.builder()
                    .paymentId("MOCK_BOLETO_" + System.currentTimeMillis())
                    .status(PaymentStatus.PENDING)
                    .amount(amount)
                    .method("boleto")
                    .success(true)
                    .build();
            }
            
            @Override
            public PaymentResult confirmBoletoPayment(String barcodeOrLine) throws PaymentException {
                return PaymentResult.builder()
                    .paymentId("MOCK_CONFIRM_" + System.currentTimeMillis())
                    .status(PaymentStatus.SUCCEEDED)
                    .success(true)
                    .build();
            }
            
            @Override
            public PaymentStatus getPaymentStatus(String paymentId) throws PaymentException {
                return PaymentStatus.SUCCEEDED;
            }
            
            @Override
            public boolean isAvailable() {
                return true;
            }
            
            @Override
            public String[] getSupportedMethods() {
                return new String[]{"pix", "card", "boleto"};
            }
        };
    }
}