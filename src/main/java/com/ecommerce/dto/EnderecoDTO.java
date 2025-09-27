package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO para transferência de dados de endereço
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnderecoDTO {
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