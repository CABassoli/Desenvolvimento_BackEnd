package com.ecommerce.integration.interfaces;

import java.util.List;

/**
 * Interface for email services following Replit integration patterns.
 * Provides abstraction for email delivery implementations.
 * 
 * Based on Replit Mail integration blueprint:
 * - Automatic token management via Replit environment
 * - Standardized email structure and validation
 * - Production-ready patterns with proper error handling
 */
public interface EmailService {
    
    /**
     * Send a simple text email.
     * 
     * @param to Recipient email address
     * @param subject Email subject
     * @param textContent Plain text email content
     * @return Email result with delivery status
     * @throws EmailException if sending fails
     */
    EmailResult sendEmail(String to, String subject, String textContent) throws EmailException;
    
    /**
     * Send an HTML email with optional text fallback.
     * 
     * @param to Recipient email address
     * @param subject Email subject
     * @param htmlContent HTML email content
     * @param textContent Plain text fallback (optional)
     * @return Email result with delivery status
     * @throws EmailException if sending fails
     */
    EmailResult sendHtmlEmail(String to, String subject, String htmlContent, String textContent) throws EmailException;
    
    /**
     * Send email to multiple recipients.
     * 
     * @param recipients List of recipient email addresses
     * @param subject Email subject
     * @param content Email content
     * @param isHtml Whether content is HTML format
     * @return Email result with delivery status for all recipients
     * @throws EmailException if sending fails
     */
    EmailResult sendBulkEmail(List<String> recipients, String subject, String content, boolean isHtml) throws EmailException;
    
    /**
     * Check if the email service is properly configured and available.
     * 
     * @return true if service is ready to send emails
     */
    boolean isAvailable();
    
    /**
     * Validate email address format.
     * 
     * @param email Email address to validate
     * @return true if email format is valid
     */
    boolean isValidEmail(String email);
}