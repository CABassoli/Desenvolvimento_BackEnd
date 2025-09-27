package com.ecommerce.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Item dentro do carrinho de compras
 */
@Entity
@Table(name = "itens_carrinho")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemCarrinho {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrinho_id", nullable = false)
    @NotNull(message = "Carrinho é obrigatório")
    private Carrinho carrinho;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    @NotNull(message = "Produto é obrigatório")
    private Produto produto;
    
    @Column(name = "quantidade", nullable = false)
    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade deve ser pelo menos 1")
    private Integer quantidade;
}