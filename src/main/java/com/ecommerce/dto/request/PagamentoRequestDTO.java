package com.ecommerce.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO base para requisições de pagamento
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PagamentoRequestDTO {
    
    @NotNull(message = "Pedido é obrigatório")
    private UUID pedidoId;
    
    @NotNull(message = "Valor é obrigatório")
    @Positive(message = "Valor deve ser positivo")
    private BigDecimal valor;
    
    @NotBlank(message = "Tipo de pagamento é obrigatório")
    private String tipoPagamento; // CARTAO, BOLETO, PIX
}