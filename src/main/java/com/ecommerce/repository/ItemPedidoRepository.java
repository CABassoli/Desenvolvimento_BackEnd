package com.ecommerce.repository;

import com.ecommerce.domain.ItemPedido;
import com.ecommerce.domain.Pedido;
import com.ecommerce.domain.Produto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para operações com itens do pedido
 */
public class ItemPedidoRepository {
    
    private final EntityManager entityManager;
    
    public ItemPedidoRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    
    /**
     * Salva ou atualiza um item do pedido
     */
    public ItemPedido save(ItemPedido item) {
        if (item.getId() == null) {
            item.setId(UUID.randomUUID());
            entityManager.persist(item);
            return item;
        } else {
            return entityManager.merge(item);
        }
    }
    
    /**
     * Busca item do pedido por ID
     */
    public Optional<ItemPedido> findById(UUID id) {
        ItemPedido item = entityManager.find(ItemPedido.class, id);
        return Optional.ofNullable(item);
    }
    
    /**
     * Lista itens por pedido
     */
    public List<ItemPedido> findByPedido(Pedido pedido) {
        TypedQuery<ItemPedido> query = entityManager.createQuery(
            "SELECT i FROM ItemPedido i WHERE i.pedido = :pedido ORDER BY i.id", ItemPedido.class);
        query.setParameter("pedido", pedido);
        return query.getResultList();
    }
    
    /**
     * Lista itens por pedido ID
     */
    public List<ItemPedido> findByPedidoId(UUID pedidoId) {
        TypedQuery<ItemPedido> query = entityManager.createQuery(
            "SELECT i FROM ItemPedido i WHERE i.pedido.id = :pedidoId ORDER BY i.id", ItemPedido.class);
        query.setParameter("pedidoId", pedidoId);
        return query.getResultList();
    }
    
    /**
     * Lista itens por produto
     */
    public List<ItemPedido> findByProduto(Produto produto) {
        TypedQuery<ItemPedido> query = entityManager.createQuery(
            "SELECT i FROM ItemPedido i WHERE i.produto = :produto ORDER BY i.pedido.dataPedido DESC", ItemPedido.class);
        query.setParameter("produto", produto);
        return query.getResultList();
    }
    
    /**
     * Lista itens por produto ID
     */
    public List<ItemPedido> findByProdutoId(UUID produtoId) {
        TypedQuery<ItemPedido> query = entityManager.createQuery(
            "SELECT i FROM ItemPedido i WHERE i.produto.id = :produtoId ORDER BY i.pedido.dataPedido DESC", ItemPedido.class);
        query.setParameter("produtoId", produtoId);
        return query.getResultList();
    }
    
    /**
     * Conta itens no pedido
     */
    public long countByPedido(UUID pedidoId) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(i) FROM ItemPedido i WHERE i.pedido.id = :pedidoId", Long.class);
        query.setParameter("pedidoId", pedidoId);
        return query.getSingleResult();
    }
    
    /**
     * Calcula quantidade total vendida de um produto
     */
    public int sumQuantidadeByProduto(UUID produtoId) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COALESCE(SUM(i.quantidade), 0) FROM ItemPedido i WHERE i.produto.id = :produtoId", Long.class);
        query.setParameter("produtoId", produtoId);
        return query.getSingleResult().intValue();
    }
    
    /**
     * Lista produtos mais vendidos
     */
    public List<Object[]> findTopSellingProducts(int limit) {
        TypedQuery<Object[]> query = entityManager.createQuery(
            "SELECT i.produto, SUM(i.quantidade) as totalVendido " +
            "FROM ItemPedido i " +
            "GROUP BY i.produto " +
            "ORDER BY totalVendido DESC", Object[].class);
        query.setMaxResults(limit);
        return query.getResultList();
    }
    
    /**
     * Remove item do pedido por ID
     */
    public void deleteById(UUID id) {
        ItemPedido item = entityManager.find(ItemPedido.class, id);
        if (item != null) {
            entityManager.remove(item);
        }
    }
    
    /**
     * Remove todos os itens do pedido
     */
    public void deleteByPedidoId(UUID pedidoId) {
        TypedQuery<ItemPedido> query = entityManager.createQuery(
            "SELECT i FROM ItemPedido i WHERE i.pedido.id = :pedidoId", ItemPedido.class);
        query.setParameter("pedidoId", pedidoId);
        List<ItemPedido> itens = query.getResultList();
        for (ItemPedido item : itens) {
            entityManager.remove(item);
        }
    }
}