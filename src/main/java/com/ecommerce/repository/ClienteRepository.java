package com.ecommerce.repository;

import com.ecommerce.domain.Cliente;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para operações com clientes
 */
public class ClienteRepository {
    
    private final EntityManager entityManager;
    
    public ClienteRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    
    /**
     * Salva ou atualiza um cliente
     */
    public Cliente save(Cliente cliente) {
        boolean needsTransaction = !entityManager.getTransaction().isActive();
        if (needsTransaction) {
            entityManager.getTransaction().begin();
        }
        
        try {
            // Use merge for both new and existing entities - let @GeneratedValue handle IDs
            cliente = entityManager.merge(cliente);
            entityManager.flush(); // Force INSERT/UPDATE to show in logs
            
            if (needsTransaction) {
                entityManager.getTransaction().commit();
            }
            
            return cliente;
        } catch (Exception e) {
            if (needsTransaction && entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw e;
        }
    }
    
    /**
     * Busca cliente por ID
     */
    public Optional<Cliente> findById(UUID id) {
        Cliente cliente = entityManager.find(Cliente.class, id);
        return Optional.ofNullable(cliente);
    }
    
    /**
     * Busca cliente por email
     */
    public Optional<Cliente> findByEmail(String email) {
        try {
            TypedQuery<Cliente> query = entityManager.createQuery(
                "SELECT c FROM Cliente c WHERE c.email = :email", Cliente.class);
            query.setParameter("email", email);
            Cliente cliente = query.getSingleResult();
            return Optional.of(cliente);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Busca clientes por nome (busca parcial)
     */
    public List<Cliente> findByNomeContaining(String nome) {
        TypedQuery<Cliente> query = entityManager.createQuery(
            "SELECT c FROM Cliente c WHERE LOWER(c.nome) LIKE LOWER(:nome) ORDER BY c.nome", Cliente.class);
        query.setParameter("nome", "%" + nome + "%");
        return query.getResultList();
    }
    
    /**
     * Lista todos os clientes
     */
    public List<Cliente> findAll() {
        TypedQuery<Cliente> query = entityManager.createQuery(
            "SELECT c FROM Cliente c ORDER BY c.nome", Cliente.class);
        return query.getResultList();
    }
    
    /**
     * Verifica se existe cliente com email
     */
    public boolean existsByEmail(String email) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(c) FROM Cliente c WHERE c.email = :email", Long.class);
        query.setParameter("email", email);
        return query.getSingleResult() > 0;
    }
    
    /**
     * Conta total de clientes
     */
    public long count() {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(c) FROM Cliente c", Long.class);
        return query.getSingleResult();
    }
    
    /**
     * Busca cliente com endereços carregados
     */
    public Optional<Cliente> findByIdWithEnderecos(UUID id) {
        try {
            TypedQuery<Cliente> query = entityManager.createQuery(
                "SELECT c FROM Cliente c LEFT JOIN FETCH c.enderecos WHERE c.id = :id", Cliente.class);
            query.setParameter("id", id);
            Cliente cliente = query.getSingleResult();
            return Optional.of(cliente);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Busca cliente com carrinho carregado
     */
    public Optional<Cliente> findByIdWithCarrinho(UUID id) {
        try {
            TypedQuery<Cliente> query = entityManager.createQuery(
                "SELECT c FROM Cliente c LEFT JOIN FETCH c.carrinho WHERE c.id = :id", Cliente.class);
            query.setParameter("id", id);
            Cliente cliente = query.getSingleResult();
            return Optional.of(cliente);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Remove cliente por ID
     */
    public void deleteById(UUID id) {
        Cliente cliente = entityManager.find(Cliente.class, id);
        if (cliente != null) {
            entityManager.remove(cliente);
        }
    }
}