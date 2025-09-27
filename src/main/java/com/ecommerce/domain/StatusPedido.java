package com.ecommerce.domain;

/**
 * Status do pedido na plataforma de e-commerce
 */
public enum StatusPedido {
    NOVO,
    PROCESSANDO,
    PAGO,
    CANCELADO,
    ENVIADO,
    ENTREGUE
}