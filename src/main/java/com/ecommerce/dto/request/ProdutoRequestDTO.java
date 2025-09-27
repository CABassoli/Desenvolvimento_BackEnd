package com.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para requisições de criação/atualização de produto
 */
@Data
public class ProdutoRequestDTO {
    
    @NotBlank(message = "Nome do produto é obrigatório")
    @Size(max = 160, message = "Nome do produto deve ter no máximo 160 caracteres")
    private String nome;
    
    @NotNull(message = "Preço é obrigatório")
    @Positive(message = "Preço deve ser positivo")
    private BigDecimal preco;
    
    @NotBlank(message = "Código de barras é obrigatório")
    @Size(min = 13, max = 13, message = "Código de barras deve ter exatamente 13 caracteres")
    private String codigoBarras;
    
    @NotNull(message = "Categoria é obrigatória")
    private UUID categoriaId;
}