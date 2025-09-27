package com.ecommerce.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Carrinho de compras do cliente
 */
@Entity
@Table(name = "carrinhos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Carrinho {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    @NotNull(message = "Cliente é obrigatório")
    @EqualsAndHashCode.Exclude
    private Cliente cliente;
    
    @OneToMany(mappedBy = "carrinho", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    private List<ItemCarrinho> itens;
}