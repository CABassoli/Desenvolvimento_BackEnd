package com.ecommerce.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para resposta com dados do produto
 */
@Data
public class ProdutoResponseDTO {
    
    private UUID id;
    private String nome;
    private BigDecimal preco;
    private String codigoBarras;
    private CategoriaResponseDTO categoria;
    private UUID categoriaId;
}