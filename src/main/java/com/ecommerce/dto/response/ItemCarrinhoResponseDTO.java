package com.ecommerce.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para resposta com dados do item do carrinho
 */
@Data
public class ItemCarrinhoResponseDTO {
    
    private UUID id;
    private ProdutoResponseDTO produto;
    private Integer quantidade;
    private BigDecimal subtotal;
}