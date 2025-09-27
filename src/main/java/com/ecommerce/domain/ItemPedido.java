package com.ecommerce.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Item de um pedido na plataforma de e-commerce
 */
@Entity
@Table(name = "itens_pedido")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedido {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    @NotNull(message = "Pedido é obrigatório")
    private Pedido pedido;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    @NotNull(message = "Produto é obrigatório")
    private Produto produto;
    
    @Column(name = "quantidade", nullable = false)
    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade deve ser pelo menos 1")
    private Integer quantidade;
    
    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Preço unitário é obrigatório")
    private BigDecimal precoUnitario;
    
    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Subtotal é obrigatório")
    private BigDecimal subtotal;
}