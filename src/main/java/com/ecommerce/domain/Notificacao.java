package com.ecommerce.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notificação enviada ao cliente
 */
@Entity
@Table(name = "notificacoes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notificacao {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    @NotNull(message = "Pedido é obrigatório")
    private Pedido pedido;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @NotNull(message = "Cliente é obrigatório")
    private Cliente cliente;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    @NotNull(message = "Tipo de notificação é obrigatório")
    private TipoNotificacao tipo;
    
    @Column(name = "mensagem", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Mensagem é obrigatória")
    private String mensagem;
    
    @Column(name = "criado_em", nullable = false)
    @NotNull
    private LocalDateTime criadoEm = LocalDateTime.now();
    
    /**
     * Tipos de notificação
     */
    public enum TipoNotificacao {
        CONFIRMACAO,
        STATUS
    }
}