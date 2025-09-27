package com.ecommerce.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ItemPedidoDTO {
    private UUID produtoId;
    private String nome;
    private Integer quantidade;
    private BigDecimal precoUnitario;
}