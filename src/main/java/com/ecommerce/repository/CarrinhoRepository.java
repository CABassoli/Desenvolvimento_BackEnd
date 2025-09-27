package com.ecommerce.repository;

import com.ecommerce.domain.Carrinho;
import com.ecommerce.domain.Cliente;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para operações com carrinho de compras
 */
public class CarrinhoRepository {
    
    private final EntityManager entityManager;
    
    public CarrinhoRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    
    /**
     * Salva ou atualiza um carrinho
     */
    public Carrinho save(Carrinho carrinho) {
        boolean needsTransaction = !entityManager.getTransaction().isActive();
        if (needsTransaction) {
            entityManager.getTransaction().begin();
        }
        
        try {
            // CRÍTICO: Reattach Cliente ao EntityManager local para evitar detached entity
            if (carrinho.getCliente() != null && carrinho.getCliente().getId() != null) {
                carrinho.setCliente(entityManager.getReference(Cliente.class, carrinho.getCliente().getId()));
            }
            
            // Use merge for both new and existing entities - more flexible than persist
            carrinho = entityManager.merge(carrinho);
            entityManager.flush(); // Force INSERT/UPDATE to show in logs
            
            if (needsTransaction) {
                entityManager.getTransaction().commit();
            }
            
            return carrinho;
        } catch (Exception e) {
            if (needsTransaction && entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw e;
        }
    }
    
    /**
     * Busca carrinho por ID
     */
    public Optional<Carrinho> findById(UUID id) {
        Carrinho carrinho = entityManager.find(Carrinho.class, id);
        return Optional.ofNullable(carrinho);
    }
    
    /**
     * Busca carrinho por cliente
     */
    public Optional<Carrinho> findByCliente(Cliente cliente) {
        try {
            TypedQuery<Carrinho> query = entityManager.createQuery(
                "SELECT c FROM Carrinho c WHERE c.cliente = :cliente", Carrinho.class);
            query.setParameter("cliente", cliente);
            Carrinho carrinho = query.getSingleResult();
            return Optional.of(carrinho);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Busca carrinho por cliente ID
     */
    public Optional<Carrinho> findByClienteId(UUID clienteId) {
        try {
            TypedQuery<Carrinho> query = entityManager.createQuery(
                "SELECT c FROM Carrinho c WHERE c.cliente.id = :clienteId", Carrinho.class);
            query.setParameter("clienteId", clienteId);
            Carrinho carrinho = query.getSingleResult();
            return Optional.of(carrinho);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Busca carrinho com itens carregados (incluindo produtos para cálculo de preço)
     */
    public Optional<Carrinho> findByIdWithItens(UUID id) {
        try {
            // CORREÇÃO DO ARQUITETO: Clear cache antes de join-fetch para evitar cache stale
            System.out.println("🧹 DEBUG: Limpando cache do EntityManager antes de consulta");
            entityManager.clear();
            
            TypedQuery<Carrinho> query = entityManager.createQuery(
                "SELECT c FROM Carrinho c LEFT JOIN FETCH c.itens i LEFT JOIN FETCH i.produto WHERE c.id = :id", Carrinho.class);
            query.setParameter("id", id);
            Carrinho carrinho = query.getSingleResult();
            return Optional.of(carrinho);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Busca carrinho por cliente com itens carregados (incluindo produtos para cálculo de preço)
     */
    public Optional<Carrinho> findByClienteIdWithItens(UUID clienteId) {
        try {
            // CORREÇÃO DO ARQUITETO: Clear cache antes de join-fetch para evitar cache stale
            System.out.println("🧹 DEBUG: Limpando cache do EntityManager antes de consulta por clienteId");
            entityManager.clear();
            
            TypedQuery<Carrinho> query = entityManager.createQuery(
                "SELECT c FROM Carrinho c LEFT JOIN FETCH c.itens i LEFT JOIN FETCH i.produto WHERE c.cliente.id = :clienteId", Carrinho.class);
            query.setParameter("clienteId", clienteId);
            Carrinho carrinho = query.getSingleResult();
            return Optional.of(carrinho);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Lista todos os carrinhos
     */
    public List<Carrinho> findAll() {
        TypedQuery<Carrinho> query = entityManager.createQuery(
            "SELECT c FROM Carrinho c ORDER BY c.id", Carrinho.class);
        return query.getResultList();
    }
    
    /**
     * Conta total de carrinhos
     */
    public long count() {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(c) FROM Carrinho c", Long.class);
        return query.getSingleResult();
    }
    
    /**
     * Remove carrinho por ID
     */
    public void deleteById(UUID id) {
        Carrinho carrinho = entityManager.find(Carrinho.class, id);
        if (carrinho != null) {
            entityManager.remove(carrinho);
        }
    }
    
    /**
     * Remove carrinho por cliente
     */
    public void deleteByClienteId(UUID clienteId) {
        TypedQuery<Carrinho> query = entityManager.createQuery(
            "SELECT c FROM Carrinho c WHERE c.cliente.id = :clienteId", Carrinho.class);
        query.setParameter("clienteId", clienteId);
        try {
            Carrinho carrinho = query.getSingleResult();
            entityManager.remove(carrinho);
        } catch (NoResultException e) {
            // Carrinho não existe, nada a fazer
        }
    }
}