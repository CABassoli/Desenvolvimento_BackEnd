package com.ecommerce.integration.interfaces;

/**
 * Interface for SMS services following Replit integration patterns.
 * Provides abstraction for SMS messaging implementations.
 * 
 * Based on Replit Twilio integration blueprint:
 * - Secure environment variable management
 * - Standardized error handling
 * - Production-ready patterns
 */
public interface SmsService {
    
    /**
     * Send SMS message to a phone number.
     * 
     * @param phoneNumber Target phone number in international format (e.g., +5511999999999)
     * @param message SMS message content
     * @return Message ID or confirmation from the SMS provider
     * @throws SmsException if sending fails
     */
    String sendMessage(String phoneNumber, String message) throws SmsException;
    
    /**
     * Check if the SMS service is properly configured and available.
     * 
     * @return true if service is ready to send messages
     */
    boolean isAvailable();
    
    /**
     * Get the configured sender phone number.
     * 
     * @return Sender phone number for this service
     */
    String getSenderNumber();
}