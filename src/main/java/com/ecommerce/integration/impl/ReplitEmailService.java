package com.ecommerce.integration.impl;

import com.ecommerce.integration.interfaces.EmailService;
import com.ecommerce.integration.interfaces.EmailException;
import com.ecommerce.integration.interfaces.EmailResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Production-ready email service using Replit's OpenInt mail service.
 * Based on official Replit Mail integration blueprint:
 * - Uses Replit's internal authentication tokens automatically
 * - Follows exact OpenInt API patterns from blueprint
 * - Production-ready with proper error handling and validation
 * 
 * Reference: blueprint:replitmail
 * Adapted from TypeScript blueprint to Java implementation
 */
public class ReplitEmailService implements com.ecommerce.integration.interfaces.EmailService {
    
    private static final String OPENINT_MAIL_ENDPOINT = "https://connectors.replit.com/api/v2/mailer/send";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String authToken;
    private boolean initialized = false;
    
    public ReplitEmailService() {
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.authToken = getAuthToken();
        
        if (isAvailable()) {
            this.initialized = true;
            System.out.println("✅ Replit Email service initialized (production-ready)");
        } else {
            System.out.println("⚠️ Replit Email service not available - missing authentication tokens");
        }
    }
    
    /**
     * Get authentication token following exact Replit blueprint pattern.
     * Based on blueprint:replitmail authentication logic.
     */
    private String getAuthToken() {
        String replIdentity = System.getenv("REPL_IDENTITY");
        if (replIdentity != null && !replIdentity.trim().isEmpty()) {
            return "repl " + replIdentity;
        }
        
        String webReplRenewal = System.getenv("WEB_REPL_RENEWAL");
        if (webReplRenewal != null && !webReplRenewal.trim().isEmpty()) {
            return "depl " + webReplRenewal;
        }
        
        return null;
    }
    
    @Override
    public EmailResult sendEmail(String to, String subject, String textContent) throws EmailException {
        if (!initialized) {
            throw new EmailException("Replit Email service not properly initialized", "NOT_INITIALIZED");
        }
        
        validateEmailInputs(to, subject, textContent);
        
        Map<String, Object> emailData = new HashMap<>();
        emailData.put("to", to);
        emailData.put("subject", subject);
        emailData.put("text", textContent);
        
        return sendEmailRequest(emailData);
    }
    
    @Override
    public EmailResult sendHtmlEmail(String to, String subject, String htmlContent, String textContent) throws EmailException {
        if (!initialized) {
            throw new EmailException("Replit Email service not properly initialized", "NOT_INITIALIZED");
        }
        
        validateEmailInputs(to, subject, htmlContent);
        
        Map<String, Object> emailData = new HashMap<>();
        emailData.put("to", to);
        emailData.put("subject", subject);
        emailData.put("html", htmlContent);
        if (textContent != null && !textContent.trim().isEmpty()) {
            emailData.put("text", textContent);
        }
        
        return sendEmailRequest(emailData);
    }
    
    @Override
    public EmailResult sendBulkEmail(List<String> recipients, String subject, String content, boolean isHtml) throws EmailException {
        if (!initialized) {
            throw new EmailException("Replit Email service not properly initialized", "NOT_INITIALIZED");
        }
        
        if (recipients == null || recipients.isEmpty()) {
            throw new EmailException("Recipients list cannot be empty", "INVALID_RECIPIENTS");
        }
        
        // Validate all recipients
        for (String recipient : recipients) {
            if (!isValidEmail(recipient)) {
                throw new EmailException("Invalid email address: " + recipient, "INVALID_EMAIL");
            }
        }
        
        validateEmailInputs(subject, content);
        
        Map<String, Object> emailData = new HashMap<>();
        emailData.put("to", recipients);
        emailData.put("subject", subject);
        if (isHtml) {
            emailData.put("html", content);
        } else {
            emailData.put("text", content);
        }
        
        return sendEmailRequest(emailData);
    }
    
    /**
     * Send email request following exact Replit blueprint pattern.
     * Uses OpenInt API endpoint with proper authentication headers.
     */
    private EmailResult sendEmailRequest(Map<String, Object> emailData) throws EmailException {
        try {
            String jsonBody = objectMapper.writeValueAsString(emailData);
            
            RequestBody requestBody = RequestBody.create(
                jsonBody, 
                MediaType.get("application/json")
            );
            
            Request request = new Request.Builder()
                .url(OPENINT_MAIL_ENDPOINT)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", authToken) // Corrected header name for Replit OpenInt API
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (!response.isSuccessful()) {
                    System.err.println("❌ Email sending failed - HTTP " + response.code() + ": " + responseBody);
                    throw new EmailException("Email sending failed: " + responseBody, "API_ERROR");
                }
                
                // Parse response following blueprint structure
                @SuppressWarnings("unchecked")
                Map<String, Object> responseData = objectMapper.readValue(responseBody, Map.class);
                
                @SuppressWarnings("unchecked")
                List<String> accepted = (List<String>) responseData.getOrDefault("accepted", Arrays.asList());
                @SuppressWarnings("unchecked")
                List<String> rejected = (List<String>) responseData.getOrDefault("rejected", Arrays.asList());
                @SuppressWarnings("unchecked")
                List<String> pending = (List<String>) responseData.getOrDefault("pending", Arrays.asList());
                String messageId = (String) responseData.getOrDefault("messageId", "");
                String responseMessage = (String) responseData.getOrDefault("response", "");
                
                boolean success = !accepted.isEmpty() || !pending.isEmpty();
                
                if (success) {
                    System.out.println("📧 Email sent successfully - MessageID: " + messageId);
                } else {
                    System.err.println("❌ Email rejected for all recipients");
                }
                
                return new EmailResult(accepted, rejected, pending, messageId, responseMessage, success);
                
            }
        } catch (IOException e) {
            System.err.println("❌ Email service network error: " + e.getMessage());
            throw new EmailException("Network error sending email", "NETWORK_ERROR", e);
        } catch (Exception e) {
            System.err.println("❌ Unexpected email error: " + e.getMessage());
            throw new EmailException("Unexpected error sending email", "UNKNOWN_ERROR", e);
        }
    }
    
    @Override
    public boolean isAvailable() {
        return authToken != null && !authToken.trim().isEmpty();
    }
    
    @Override
    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
    
    private void validateEmailInputs(String to, String subject, String content) throws EmailException {
        if (!isValidEmail(to)) {
            throw new EmailException("Invalid recipient email address: " + to, "INVALID_EMAIL");
        }
        validateEmailInputs(subject, content);
    }
    
    private void validateEmailInputs(String subject, String content) throws EmailException {
        if (subject == null || subject.trim().isEmpty()) {
            throw new EmailException("Email subject is required", "INVALID_SUBJECT");
        }
        
        if (content == null || content.trim().isEmpty()) {
            throw new EmailException("Email content is required", "INVALID_CONTENT");
        }
    }
}