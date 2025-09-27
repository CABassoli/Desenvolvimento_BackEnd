package com.ecommerce.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO para resposta com dados do carrinho
 */
@Data
public class CarrinhoResponseDTO {
    
    private UUID id;
    private UUID userId;
    private UUID clienteId;
    private List<ItemCarrinhoResponseDTO> itens;
    private BigDecimal valorTotal;
    private Integer totalItens;
    private BigDecimal totalValor;
    private Integer totalQuantidade;
}