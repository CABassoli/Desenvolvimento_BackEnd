package com.ecommerce.dto.response;

import com.ecommerce.domain.StatusPedido;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class PedidoResponseDTO {
    
    private UUID id;
    private String numero;
    private String idempotencyKey;
    private LocalDateTime dataPedido;
    private StatusPedido status;
    private BigDecimal valorTotal;
    private BigDecimal total;
    private Instant etaEntrega;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant paidAt;
    private Instant canceledAt;
    private long version;
    private ClienteResponseDTO cliente;
    private EnderecoResponseDTO endereco;
    private List<ItemPedidoResponseDTO> itens;
    private List<ItemPedidoResponseDTO> pedidoItens;
}