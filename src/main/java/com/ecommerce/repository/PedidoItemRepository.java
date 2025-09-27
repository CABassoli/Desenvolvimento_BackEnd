package com.ecommerce.repository;

import com.ecommerce.domain.PedidoItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PedidoItemRepository {
    
    private final EntityManager entityManager;
    
    public PedidoItemRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    
    public PedidoItem save(PedidoItem pedidoItem) {
        try {
            if (pedidoItem.getId() == null) {
                pedidoItem.setId(UUID.randomUUID());
                entityManager.persist(pedidoItem);
            } else {
                pedidoItem = entityManager.merge(pedidoItem);
            }
            return pedidoItem;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao salvar item do pedido: " + e.getMessage(), e);
        }
    }
    
    public Optional<PedidoItem> findById(UUID id) {
        PedidoItem pedidoItem = entityManager.find(PedidoItem.class, id);
        return Optional.ofNullable(pedidoItem);
    }
    
    public List<PedidoItem> findByPedidoId(UUID pedidoId) {
        TypedQuery<PedidoItem> query = entityManager.createQuery(
            "SELECT pi FROM PedidoItem pi WHERE pi.pedidoId = :pedidoId", PedidoItem.class);
        query.setParameter("pedidoId", pedidoId);
        return query.getResultList();
    }
    
    public void deleteById(UUID id) {
        PedidoItem pedidoItem = entityManager.find(PedidoItem.class, id);
        if (pedidoItem != null) {
            entityManager.remove(pedidoItem);
        }
    }
}