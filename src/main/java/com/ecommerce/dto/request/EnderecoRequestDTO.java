package com.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.util.UUID;

/**
 * DTO para requisições de criação/atualização de endereço
 */
@Data
public class EnderecoRequestDTO {
    
    @NotBlank(message = "Rua é obrigatória")
    private String rua;
    
    @NotBlank(message = "Número é obrigatório")
    private String numero;
    
    @NotBlank(message = "CEP é obrigatório")
    @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP deve ter formato válido (XXXXX-XXX)")
    private String cep;
    
    @NotBlank(message = "Cidade é obrigatória")
    private String cidade;
    
    @NotBlank(message = "Bairro é obrigatório")
    private String bairro;
    
    @NotBlank(message = "Estado é obrigatório")
    private String estado;
    
    private String complemento;
    
    private Boolean ehPadrao;
    
    @NotNull(message = "Cliente é obrigatório")
    private UUID clienteId;
}