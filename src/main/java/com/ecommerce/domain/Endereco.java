package com.ecommerce.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Endereço de entrega do cliente
 */
@Entity
@Table(name = "enderecos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Endereco {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @NotNull(message = "Cliente é obrigatório")
    private Cliente cliente;
    
    @Column(name = "rua", nullable = false)
    @NotBlank(message = "Rua é obrigatória")
    private String rua;
    
    @Column(name = "numero", nullable = false)
    @NotBlank(message = "Número é obrigatório")
    private String numero;
    
    @Column(name = "complemento")
    private String complemento;
    
    @Column(name = "bairro", nullable = false)
    @NotBlank(message = "Bairro é obrigatório")
    private String bairro;
    
    @Column(name = "cidade", nullable = false)
    @NotBlank(message = "Cidade é obrigatória")
    private String cidade;
    
    @Column(name = "estado", nullable = false, length = 2)
    @NotBlank(message = "Estado é obrigatório")
    private String estado;
    
    @Column(name = "cep", nullable = false)
    @NotBlank(message = "CEP é obrigatório")
    private String cep;
    
    @Column(name = "eh_padrao", nullable = false)
    private Boolean ehPadrao = false;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
        if (ehPadrao == null) ehPadrao = false;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}