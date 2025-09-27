package com.ecommerce.integration.interfaces;

import java.math.BigDecimal;

/**
 * Interface for payment services following Replit integration patterns.
 * Provides abstraction for payment processing implementations.
 * 
 * Based on Replit Stripe integration blueprint:
 * - Secure API key management via environment variables
 * - Production-ready payment processing patterns
 * - Proper error handling and validation
 */
public interface PaymentService {
    
    /**
     * Process PIX payment.
     * 
     * @param pedidoId Order ID for payment reference
     * @param amount Payment amount
     * @param description Payment description
     * @return Payment result with PIX code/QR
     * @throws PaymentException if processing fails
     */
    PaymentResult processPixPayment(Long pedidoId, BigDecimal amount, String description) throws PaymentException;
    
    /**
     * Process credit card payment.
     * 
     * @param pedidoId Order ID for payment reference
     * @param amount Payment amount
     * @param cardData Encrypted card data
     * @param description Payment description
     * @return Payment result with confirmation
     * @throws PaymentException if processing fails
     */
    PaymentResult processCardPayment(Long pedidoId, BigDecimal amount, CardData cardData, String description) throws PaymentException;
    
    /**
     * Generate boleto (bank slip) for payment.
     * 
     * @param pedidoId Order ID for payment reference
     * @param amount Payment amount
     * @param customerData Customer information for boleto
     * @param description Payment description
     * @return Payment result with boleto details
     * @throws PaymentException if generation fails
     */
    PaymentResult generateBoleto(Long pedidoId, BigDecimal amount, CustomerData customerData, String description) throws PaymentException;
    
    /**
     * Confirm boleto payment using barcode/line.
     * 
     * @param barcodeOrLine Boleto barcode or digital line
     * @return Payment confirmation result
     * @throws PaymentException if confirmation fails
     */
    PaymentResult confirmBoletoPayment(String barcodeOrLine) throws PaymentException;
    
    /**
     * Check payment status.
     * 
     * @param paymentId Payment ID to check
     * @return Current payment status
     * @throws PaymentException if status check fails
     */
    PaymentStatus getPaymentStatus(String paymentId) throws PaymentException;
    
    /**
     * Check if the payment service is properly configured and available.
     * 
     * @return true if service is ready to process payments
     */
    boolean isAvailable();
    
    /**
     * Get supported payment methods for this service.
     * 
     * @return Array of supported payment method names
     */
    String[] getSupportedMethods();
}