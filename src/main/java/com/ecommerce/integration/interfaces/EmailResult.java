package com.ecommerce.integration.interfaces;

import java.util.List;

/**
 * Result object for email operations.
 * Based on Replit Mail service response structure.
 */
public class EmailResult {
    
    private final List<String> accepted;
    private final List<String> rejected;
    private final List<String> pending;
    private final String messageId;
    private final String response;
    private final boolean success;
    
    public EmailResult(List<String> accepted, List<String> rejected, List<String> pending, 
                      String messageId, String response, boolean success) {
        this.accepted = accepted;
        this.rejected = rejected;
        this.pending = pending;
        this.messageId = messageId;
        this.response = response;
        this.success = success;
    }
    
    public List<String> getAccepted() {
        return accepted;
    }
    
    public List<String> getRejected() {
        return rejected;
    }
    
    public List<String> getPending() {
        return pending;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public String getResponse() {
        return response;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    @Override
    public String toString() {
        return "EmailResult{" +
                "accepted=" + accepted +
                ", rejected=" + rejected +
                ", pending=" + pending +
                ", messageId='" + messageId + '\'' +
                ", success=" + success +
                '}';
    }
}