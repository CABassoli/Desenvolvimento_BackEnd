package com.ecommerce.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para requisições de criação/atualização de cliente
 */
@Data
public class ClienteRequestDTO {
    
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 160, message = "Nome deve ter no máximo 160 caracteres")
    private String nome;
    
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter formato válido")
    @Size(max = 160, message = "Email deve ter no máximo 160 caracteres")
    private String email;
}