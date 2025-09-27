package com.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para dados de pagamento na confirmação de pedido
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PagamentoConfirmacaoDTO {
    
    @NotBlank(message = "Método de pagamento é obrigatório")
    private String metodo; // "CARTAO" ou "BOLETO"
    
    private String tokenCartao; // Obrigatório se metodo = CARTAO
    
    private String bandeira; // Obrigatório se metodo = CARTAO (ex: "VISA", "MASTERCARD")
}