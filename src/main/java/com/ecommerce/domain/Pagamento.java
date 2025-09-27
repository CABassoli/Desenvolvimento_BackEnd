package com.ecommerce.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Classe abstrata para pagamentos
 */
@Entity
@Table(name = "pagamentos")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class Pagamento {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    @NotNull(message = "Pedido é obrigatório")
    private Pedido pedido;
    
    @Column(name = "valor", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.0", inclusive = true, message = "Valor deve ser maior ou igual a zero")
    private BigDecimal valor;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo", nullable = false)
    @NotNull(message = "Método de pagamento é obrigatório")
    private MetodoPagamento metodo;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull(message = "Status do pagamento é obrigatório")
    private StatusPagamento status = StatusPagamento.PENDENTE;
    
    @Column(name = "nsu")
    private String nsu;
    
    @Column(name = "linha_digitavel")
    private String linhaDigitavel;
    
    @Column(name = "transacao_id")
    private String transacaoId;
    
    @Column(name = "mensagem")
    private String mensagem;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = StatusPagamento.PENDENTE;
    }
}