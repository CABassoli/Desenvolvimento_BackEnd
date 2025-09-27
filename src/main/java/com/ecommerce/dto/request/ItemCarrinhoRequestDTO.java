package com.ecommerce.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

/**
 * DTO para requisições de adição/atualização de item no carrinho
 */
@Data
public class ItemCarrinhoRequestDTO {
    
    @NotNull(message = "Produto é obrigatório")
    private UUID produtoId;
    
    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade deve ser pelo menos 1")
    private Integer quantidade;
}