package com.ecommerce.dto;

import com.ecommerce.domain.MetodoPagamento;
import com.ecommerce.domain.StatusPagamento;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO para resposta de simulação de pagamento
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimulacaoPagamentoResponseDTO {
    
    private MetodoPagamento metodo;
    private StatusPagamento status;
    private BigDecimal valor;
    private String nsu;
    private String transacaoId;
    private String linhaDigitavel;
    private String mensagem;
    private Instant dataProcessamento;
    private Instant dataVencimento;
    
    // Dados adicionais para cartão
    private String bandeira;
    private String ultimosDigitosCartao;
    private String tokenCartao;  // Token gerado para o cartão
}