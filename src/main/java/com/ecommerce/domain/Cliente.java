package com.ecommerce.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Cliente da plataforma de e-commerce
 */
@Entity
@Table(name = "clientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {
    
    @Id
    private UUID id;
    
    @Column(name = "nome", length = 160, nullable = false)
    @NotBlank(message = "Nome do cliente é obrigatório")
    @Size(max = 160, message = "Nome deve ter no máximo 160 caracteres")
    private String nome;
    
    @Column(name = "email", length = 160, nullable = false, unique = true)
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter formato válido")
    @Size(max = 160, message = "Email deve ter no máximo 160 caracteres")
    private String email;
    
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    private List<Endereco> enderecos;
    
    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    private Carrinho carrinho;
    
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    private List<Pedido> pedidos;
}