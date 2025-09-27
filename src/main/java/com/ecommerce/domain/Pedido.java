package com.ecommerce.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pedidos", indexes = {
    @Index(name = "idx_pedido_created_at", columnList = "created_at"),
    @Index(name = "idx_pedido_status", columnList = "status"),
    @Index(name = "idx_pedido_idempotency_key", columnList = "idempotency_key", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;
    
    @Column(name = "idempotency_key", unique = true, nullable = true)
    private String idempotencyKey;
    
    @Column(name = "numero", unique = true)
    private String numero;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", insertable = false, updatable = false)
    private Cliente cliente;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endereco_id", nullable = true)
    private Endereco enderecoEntrega;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusPedido status = StatusPedido.NOVO;
    
    @Column(name = "eta_entrega")
    private Instant etaEntrega;
    
    @Column(name = "data_pedido")
    private LocalDateTime dataPedido;
    
    @Column(name = "valor_total", precision = 10, scale = 2)
    private BigDecimal valorTotal;
    
    @Column(name = "total", nullable = false, precision = 19, scale = 2)
    private BigDecimal total;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @Column(name = "paid_at")
    private Instant paidAt;
    
    @Column(name = "canceled_at")
    private Instant canceledAt;
    
    // Payment metadata columns
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pagamento")
    private MetodoPagamento metodoPagamento;
    
    @Column(name = "transaction_id")
    private String transactionId;
    
    @Column(name = "payment_status")
    private String paymentStatus;
    
    @Version
    @Column(name = "version")
    private Long version;
    
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PedidoItem> itens = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        // NÃO definir ID - deixar @GeneratedValue fazer isso
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
        if (dataPedido == null) dataPedido = LocalDateTime.now();
        if (status == null) status = StatusPedido.NOVO;
        if (numero == null) {
            // Generate unique order number based on timestamp and random suffix
            numero = "PED" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    // Método auxiliar para adicionar item mantendo relacionamento bidirecional
    public void addItem(PedidoItem item) {
        item.setPedido(this);
        this.itens.add(item);
    }
}