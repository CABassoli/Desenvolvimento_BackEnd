package com.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO para requisição de confirmação de pedido
 * Estrutura esperada: { enderecoId, pagamento: { metodo, tokenCartao, bandeira } }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // Tolerância a campos extras do frontend
public class ConfirmarPedidoRequestDTO {
    
    @NotNull(message = "ID do endereço é obrigatório")
    private UUID enderecoId;
    
    @NotNull(message = "Dados de pagamento são obrigatórios")
    @Valid
    private PagamentoConfirmacaoDTO pagamento;
    
    // Chave de idempotência (opcional - para evitar duplicação de pedidos)
    private String idempotencyKey;
}