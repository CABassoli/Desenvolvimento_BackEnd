package com.ecommerce.dto.response;

import lombok.Data;
import java.util.UUID;

/**
 * DTO para resposta com dados do cliente
 */
@Data
public class ClienteResponseDTO {
    
    private UUID id;
    private String nome;
    private String email;
}