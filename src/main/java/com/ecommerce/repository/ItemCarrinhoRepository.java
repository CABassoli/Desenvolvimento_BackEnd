package com.ecommerce.repository;

import com.ecommerce.domain.Carrinho;
import com.ecommerce.domain.ItemCarrinho;
import com.ecommerce.domain.Produto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para operações com itens do carrinho
 */
public class ItemCarrinhoRepository {
    
    private final EntityManager defaultEntityManager;
    
    public ItemCarrinhoRepository(EntityManager defaultEntityManager) {
        this.defaultEntityManager = defaultEntityManager;
    }
    
    /**
     * Obtém o EntityManager apropriado (do request atual se disponível)
     */
    private EntityManager getEntityManager() {
        try {
            // Tenta obter o EntityManager do request atual (com transação ativa)
            return com.ecommerce.config.DatabaseConfig.getEntityManager();
        } catch (IllegalStateException e) {
            // Se não houver request ativo, usa o default
            return defaultEntityManager;
        }
    }
    
    /**
     * Salva ou atualiza um item do carrinho
     */
    public ItemCarrinho save(ItemCarrinho item) {
        EntityManager em = getEntityManager();
        // Transação já deve estar ativa pelo TransactionFilter
        try {
            // Use merge for both new and existing entities - more flexible than persist
            item = em.merge(item);
            em.flush(); // Force INSERT/UPDATE to show in logs
            return item;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao salvar item do carrinho: " + e.getMessage(), e);
        }
    }
    
    /**
     * Busca item do carrinho por ID
     */
    public Optional<ItemCarrinho> findById(UUID id) {
        ItemCarrinho item = getEntityManager().find(ItemCarrinho.class, id);
        return Optional.ofNullable(item);
    }
    
    /**
     * Lista itens por carrinho
     */
    public List<ItemCarrinho> findByCarrinho(Carrinho carrinho) {
        TypedQuery<ItemCarrinho> query = getEntityManager().createQuery(
            "SELECT i FROM ItemCarrinho i WHERE i.carrinho = :carrinho ORDER BY i.id", ItemCarrinho.class);
        query.setParameter("carrinho", carrinho);
        return query.getResultList();
    }
    
    /**
     * Lista itens por carrinho ID
     */
    public List<ItemCarrinho> findByCarrinhoId(UUID carrinhoId) {
        TypedQuery<ItemCarrinho> query = getEntityManager().createQuery(
            "SELECT i FROM ItemCarrinho i WHERE i.carrinho.id = :carrinhoId ORDER BY i.id", ItemCarrinho.class);
        query.setParameter("carrinhoId", carrinhoId);
        return query.getResultList();
    }
    
    /**
     * Busca item específico no carrinho por produto
     */
    public Optional<ItemCarrinho> findByCarrinhoAndProduto(Carrinho carrinho, Produto produto) {
        try {
            TypedQuery<ItemCarrinho> query = getEntityManager().createQuery(
                "SELECT i FROM ItemCarrinho i WHERE i.carrinho = :carrinho AND i.produto = :produto", ItemCarrinho.class);
            query.setParameter("carrinho", carrinho);
            query.setParameter("produto", produto);
            ItemCarrinho item = query.getSingleResult();
            return Optional.of(item);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Busca item específico no carrinho por produto ID
     */
    public Optional<ItemCarrinho> findByCarrinhoIdAndProdutoId(UUID carrinhoId, UUID produtoId) {
        try {
            TypedQuery<ItemCarrinho> query = getEntityManager().createQuery(
                "SELECT i FROM ItemCarrinho i WHERE i.carrinho.id = :carrinhoId AND i.produto.id = :produtoId", ItemCarrinho.class);
            query.setParameter("carrinhoId", carrinhoId);
            query.setParameter("produtoId", produtoId);
            ItemCarrinho item = query.getSingleResult();
            return Optional.of(item);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Conta itens no carrinho
     */
    public long countByCarrinho(UUID carrinhoId) {
        TypedQuery<Long> query = getEntityManager().createQuery(
            "SELECT COUNT(i) FROM ItemCarrinho i WHERE i.carrinho.id = :carrinhoId", Long.class);
        query.setParameter("carrinhoId", carrinhoId);
        return query.getSingleResult();
    }
    
    /**
     * Calcula quantidade total de itens no carrinho
     */
    public int sumQuantidadeByCarrinho(UUID carrinhoId) {
        TypedQuery<Long> query = getEntityManager().createQuery(
            "SELECT COALESCE(SUM(i.quantidade), 0) FROM ItemCarrinho i WHERE i.carrinho.id = :carrinhoId", Long.class);
        query.setParameter("carrinhoId", carrinhoId);
        return query.getSingleResult().intValue();
    }
    
    /**
     * Verifica se existe item no carrinho
     */
    public boolean existsByCarrinhoIdAndProdutoId(UUID carrinhoId, UUID produtoId) {
        TypedQuery<Long> query = getEntityManager().createQuery(
            "SELECT COUNT(i) FROM ItemCarrinho i WHERE i.carrinho.id = :carrinhoId AND i.produto.id = :produtoId", Long.class);
        query.setParameter("carrinhoId", carrinhoId);
        query.setParameter("produtoId", produtoId);
        return query.getSingleResult() > 0;
    }
    
    /**
     * Remove item do carrinho por ID
     */
    public void deleteById(UUID id) {
        EntityManager em = getEntityManager();
        // Transação já deve estar ativa pelo TransactionFilter
        try {
            // Usar query DELETE diretamente para garantir que funciona
            int deletedCount = em.createQuery(
                "DELETE FROM ItemCarrinho i WHERE i.id = :id")
                .setParameter("id", id)
                .executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao remover item do carrinho", e);
        }
    }
    
    /**
     * Remove todos os itens do carrinho
     */
    public void deleteByCarrinhoId(UUID carrinhoId) {
        EntityManager em = getEntityManager();
        // Transação já deve estar ativa pelo TransactionFilter
        try {
            // Usar query DELETE em massa para melhor performance
            int deletedCount = em.createQuery(
                "DELETE FROM ItemCarrinho i WHERE i.carrinho.id = :carrinhoId")
                .setParameter("carrinhoId", carrinhoId)
                .executeUpdate();
            
            System.out.println("✅ Removidos " + deletedCount + " itens do carrinho: " + carrinhoId);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao limpar carrinho", e);
        }
    }
}