package com.ecommerce.dto.response;

import lombok.Data;
import java.util.UUID;

/**
 * DTO para resposta com dados da categoria
 */
@Data
public class CategoriaResponseDTO {
    
    private UUID id;
    private String nome;
}