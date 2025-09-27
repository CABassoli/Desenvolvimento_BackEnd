package com.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para requisições de criação/atualização de categoria
 */
@Data
public class CategoriaRequestDTO {
    
    @NotBlank(message = "Nome da categoria é obrigatório")
    @Size(max = 120, message = "Nome da categoria deve ter no máximo 120 caracteres")
    private String nome;
}