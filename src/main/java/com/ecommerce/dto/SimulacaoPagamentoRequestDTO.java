package com.ecommerce.dto;

import com.ecommerce.domain.MetodoPagamento;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para requisição de simulação de pagamento
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimulacaoPagamentoRequestDTO {
    
    @NotNull(message = "Método de pagamento é obrigatório")
    private String metodo; // "CARTAO" ou "BOLETO"
    
    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    private BigDecimal valor;
    
    // Dados do cartão (obrigatório quando metodo = CARTAO)
    private DadosCartaoDTO dadosCartao;
}