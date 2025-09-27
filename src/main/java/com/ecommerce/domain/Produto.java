package com.ecommerce.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Produto disponível na plataforma de e-commerce
 */
@Entity
@Table(name = "produtos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Produto {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "codigo_barras", length = 13, nullable = false, unique = true)
    @NotBlank(message = "Código de barras é obrigatório")
    @Size(min = 13, max = 13, message = "Código de barras deve ter exatamente 13 caracteres")
    private String codigoBarras;
    
    @Column(name = "nome", length = 160, nullable = false)
    @NotBlank(message = "Nome do produto é obrigatório")
    @Size(max = 160, message = "Nome do produto deve ter no máximo 160 caracteres")
    private String nome;
    
    @Column(name = "preco", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Preço é obrigatório")
    @DecimalMin(value = "0.0", inclusive = true, message = "Preço deve ser maior ou igual a zero")
    private BigDecimal preco;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    @NotNull(message = "Categoria é obrigatória")
    private Categoria categoria;
}