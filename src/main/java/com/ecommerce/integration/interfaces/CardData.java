package com.ecommerce.integration.interfaces;

/**
 * Card data for payment processing.
 * Contains encrypted/tokenized card information for secure payment processing.
 */
public class CardData {
    
    private final String token;
    private final String last4Digits;
    private final String brand;
    private final String expiryMonth;
    private final String expiryYear;
    private final String holderName;
    private final String cvv;
    private final boolean encrypted;
    
    private CardData(Builder builder) {
        this.token = builder.token;
        this.last4Digits = builder.last4Digits;
        this.brand = builder.brand;
        this.expiryMonth = builder.expiryMonth;
        this.expiryYear = builder.expiryYear;
        this.holderName = builder.holderName;
        this.cvv = builder.cvv;
        this.encrypted = builder.encrypted;
    }
    
    // Getters
    public String getToken() { return token; }
    public String getLast4Digits() { return last4Digits; }
    public String getBrand() { return brand; }
    public String getExpiryMonth() { return expiryMonth; }
    public String getExpiryYear() { return expiryYear; }
    public String getHolderName() { return holderName; }
    public String getCvv() { return cvv; }
    public boolean isEncrypted() { return encrypted; }
    
    public static class Builder {
        private String token;
        private String last4Digits;
        private String brand;
        private String expiryMonth;
        private String expiryYear;
        private String holderName;
        private String cvv;
        private boolean encrypted = true;
        
        public Builder token(String token) { this.token = token; return this; }
        public Builder last4Digits(String last4Digits) { this.last4Digits = last4Digits; return this; }
        public Builder brand(String brand) { this.brand = brand; return this; }
        public Builder expiryMonth(String expiryMonth) { this.expiryMonth = expiryMonth; return this; }
        public Builder expiryYear(String expiryYear) { this.expiryYear = expiryYear; return this; }
        public Builder holderName(String holderName) { this.holderName = holderName; return this; }
        public Builder cvv(String cvv) { this.cvv = cvv; return this; }
        public Builder encrypted(boolean encrypted) { this.encrypted = encrypted; return this; }
        
        public CardData build() {
            return new CardData(this);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public String toString() {
        return "CardData{" +
                "last4Digits='" + last4Digits + '\'' +
                ", brand='" + brand + '\'' +
                ", encrypted=" + encrypted +
                '}';
    }
}