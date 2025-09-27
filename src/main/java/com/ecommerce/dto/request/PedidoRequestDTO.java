package com.ecommerce.dto.request;

import com.ecommerce.dto.ItemPedidoDTO;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class PedidoRequestDTO {
    
    @NotNull(message = "Cliente é obrigatório")
    private UUID clienteId;
    
    @NotNull(message = "Endereço de entrega é obrigatório")
    private UUID enderecoId;
    
    private String idempotencyKey;
    
    private String origem;
    
    private List<ItemPedidoDTO> itens;
}