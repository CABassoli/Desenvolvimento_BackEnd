package com.ecommerce.dto.response;

import lombok.Data;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO para resposta com dados do endereço
 */
@Data
public class EnderecoResponseDTO {
    
    private UUID id;
    private UUID clienteId;
    private String rua;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String estado;
    private String cep;
    private Boolean ehPadrao;
    private Instant createdAt;
    private Instant updatedAt;
}