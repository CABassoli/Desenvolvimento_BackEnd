package com.ecommerce.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para resposta com dados do pagamento
 */
@Data
public class PagamentoResponseDTO {
    
    private UUID id;
    private UUID pedidoId;
    private BigDecimal valor;
    private String tipoPagamento;
    
    // Campos específicos dependendo do tipo
    private String tokenCartao;
    private String bandeira;
    private String linhaDigitavel;
    private String txid;
}