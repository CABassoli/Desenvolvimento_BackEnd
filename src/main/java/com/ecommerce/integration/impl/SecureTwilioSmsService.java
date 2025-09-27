package com.ecommerce.integration.impl;

import com.ecommerce.integration.interfaces.SmsService;
import com.ecommerce.integration.interfaces.SmsException;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

/**
 * Production-ready Twilio SMS service implementation.
 * Based on official Replit Twilio integration blueprint:
 * - Uses environment variables for secure credential management
 * - Follows Replit integration patterns for error handling
 * - Production-ready with proper validation and logging
 * 
 * Reference: blueprint:twilio_send_message
 */
public class SecureTwilioSmsService implements com.ecommerce.integration.interfaces.SmsService {
    
    private final String accountSid;
    private final String authToken;
    private final String fromPhoneNumber;
    private boolean initialized = false;
    
    public SecureTwilioSmsService() {
        // Following Replit blueprint pattern for environment variable management
        this.accountSid = System.getenv("TWILIO_ACCOUNT_SID");
        this.authToken = System.getenv("TWILIO_AUTH_TOKEN");
        this.fromPhoneNumber = System.getenv("TWILIO_PHONE_NUMBER");
        
        if (isAvailable()) {
            System.out.println("✅ Twilio SMS service configured (lazy initialization)");
        } else {
            System.out.println("⚠️ Twilio SMS service not configured - missing environment variables");
        }
    }
    
    @Override
    public String sendMessage(String phoneNumber, String message) throws SmsException {
        // Initialize Twilio lazily when actually needed
        initializeTwilioIfNeeded();
        
        if (!initialized) {
            throw new SmsException("Twilio SMS service not properly initialized", "NOT_INITIALIZED");
        }
        
        // Validate inputs
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new SmsException("Phone number is required", "INVALID_PHONE");
        }
        
        if (message == null || message.trim().isEmpty()) {
            throw new SmsException("Message content is required", "INVALID_MESSAGE");
        }
        
        // Format and validate phone number using E.164 standard
        String formattedPhone = formatToE164(phoneNumber);
        if (formattedPhone == null) {
            throw new SmsException("Invalid phone number format: " + phoneNumber, "INVALID_PHONE_FORMAT");
        }
        
        try {
            // Following Replit blueprint pattern for message sending
            Message twilioMessage = Message.creator(
                new PhoneNumber(formattedPhone),
                new PhoneNumber(fromPhoneNumber),
                message
            ).create();
            
            System.out.println("📱 SMS sent successfully - SID: " + twilioMessage.getSid());
            return twilioMessage.getSid();
            
        } catch (com.twilio.exception.TwilioException e) {
            String errorCode = "TWILIO_ERROR";
            if (e instanceof com.twilio.exception.ApiException) {
                errorCode = String.valueOf(((com.twilio.exception.ApiException) e).getCode());
            }
            
            System.err.println("❌ Twilio SMS failed: " + e.getMessage() + " (Code: " + errorCode + ")");
            throw new SmsException("Failed to send SMS: " + e.getMessage(), errorCode, e);
            
        } catch (Exception e) {
            System.err.println("❌ Unexpected SMS error: " + e.getMessage());
            throw new SmsException("Unexpected error sending SMS", "UNKNOWN_ERROR", e);
        }
    }
    
    @Override
    public boolean isAvailable() {
        return accountSid != null && !accountSid.trim().isEmpty() &&
               authToken != null && !authToken.trim().isEmpty() &&
               fromPhoneNumber != null && !fromPhoneNumber.trim().isEmpty();
    }
    
    @Override
    public String getSenderNumber() {
        return fromPhoneNumber;
    }
    
    /**
     * Initialize Twilio only when actually needed (lazy initialization)
     */
    private void initializeTwilioIfNeeded() throws SmsException {
        if (isAvailable() && !initialized) {
            synchronized (this) {
                if (!initialized) {
                    try {
                        Twilio.init(accountSid, authToken);
                        this.initialized = true;
                        System.out.println("✅ Twilio SMS service initialized on-demand");
                    } catch (Exception e) {
                        System.err.println("❌ Twilio initialization failed: " + e.getMessage());
                        throw new SmsException("Failed to initialize Twilio service", "INITIALIZATION_FAILED", e);
                    }
                }
            }
        }
    }
    
    /**
     * Format phone number to E.164 standard with Brazilian validation.
     * Based on existing TwilioSmsService.formatBrazilianPhone but enhanced.
     */
    private String formatToE164(String phone) {
        if (phone == null) return null;
        
        // Handle already formatted international numbers
        if (phone.startsWith("+")) {
            String cleanPhone = phone.replaceAll("[^0-9+]", "");
            if (cleanPhone.length() >= 11 && cleanPhone.length() <= 16) {
                return cleanPhone;
            }
        }
        
        // Remove all non-numeric characters
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        
        // Handle Brazilian phone numbers with proper E.164 formatting
        if (cleanPhone.length() == 13 && cleanPhone.startsWith("55")) {
            // Already has country code (5511987654321)
            return "+" + cleanPhone;
        } else if (cleanPhone.length() == 11) {
            // Brazilian mobile: 11987654321
            if (cleanPhone.charAt(2) == '9') {
                // Already has 9 digit (correct modern format)
                return "+55" + cleanPhone;
            } else {
                // Missing 9 digit - this is likely landline, not mobile
                // For safety, reject landlines for SMS
                return null;
            }
        } else if (cleanPhone.length() == 10) {
            // Old mobile format without 9: 1187654321
            // Add 9 to make it modern format: 11987654321
            return "+55" + cleanPhone.substring(0, 2) + "9" + cleanPhone.substring(2);
        }
        
        // For non-Brazilian numbers, ensure + prefix and validate length
        if (cleanPhone.length() >= 10 && cleanPhone.length() <= 15) {
            return "+" + cleanPhone;
        }
        
        return null; // Invalid format
    }
    
    /**
     * Determine if a Twilio error is retryable based on error codes.
     * Following production best practices for retry logic.
     */
    private boolean isRetryableError(com.twilio.exception.TwilioException e) {
        if (!(e instanceof com.twilio.exception.ApiException)) {
            return false;
        }
        
        int code = ((com.twilio.exception.ApiException) e).getCode();
        // Retryable errors: rate limits, server errors, temporary failures
        return code >= 20429 && code <= 20599 || // Rate limits and server errors
               code == 21611 || // Phone number not reachable (temporary)
               code >= 30001 && code <= 30099; // Queue and delivery issues
    }
}