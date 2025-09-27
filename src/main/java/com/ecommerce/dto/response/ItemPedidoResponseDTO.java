package com.ecommerce.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para resposta com dados do item do pedido
 */
@Data
public class ItemPedidoResponseDTO {
    
    private UUID id;
    private ProdutoResponseDTO produto;
    private Integer quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal subtotal;
}