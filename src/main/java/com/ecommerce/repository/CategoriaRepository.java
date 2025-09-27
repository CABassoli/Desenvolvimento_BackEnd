package com.ecommerce.repository;

import com.ecommerce.domain.Categoria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para operações com categorias
 */
public class CategoriaRepository {
    
    private final EntityManager entityManager;
    
    public CategoriaRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    
    /**
     * Salva ou atualiza uma categoria
     */
    public Categoria save(Categoria categoria) {
        boolean needsTransaction = !entityManager.getTransaction().isActive();
        if (needsTransaction) {
            entityManager.getTransaction().begin();
        }
        
        try {
            // Use merge for both new and existing entities - more flexible than persist
            categoria = entityManager.merge(categoria);
            entityManager.flush(); // Force INSERT/UPDATE to show in logs
            
            if (needsTransaction) {
                entityManager.getTransaction().commit();
            }
            
            return categoria;
        } catch (Exception e) {
            if (needsTransaction && entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw e;
        }
    }
    
    /**
     * Busca categoria por ID
     */
    public Optional<Categoria> findById(UUID id) {
        Categoria categoria = entityManager.find(Categoria.class, id);
        return Optional.ofNullable(categoria);
    }
    
    /**
     * Busca categoria por nome
     */
    public Optional<Categoria> findByNome(String nome) {
        try {
            TypedQuery<Categoria> query = entityManager.createQuery(
                "SELECT c FROM Categoria c WHERE c.nome = :nome", Categoria.class);
            query.setParameter("nome", nome);
            Categoria categoria = query.getSingleResult();
            return Optional.of(categoria);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Lista todas as categorias ordenadas por nome
     */
    public List<Categoria> findAll() {
        TypedQuery<Categoria> query = entityManager.createQuery(
            "SELECT c FROM Categoria c ORDER BY c.nome", Categoria.class);
        return query.getResultList();
    }
    
    /**
     * Verifica se existe categoria com nome
     */
    public boolean existsByNome(String nome) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(c) FROM Categoria c WHERE c.nome = :nome", Long.class);
        query.setParameter("nome", nome);
        return query.getSingleResult() > 0;
    }
    
    /**
     * Conta total de categorias
     */
    public long count() {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(c) FROM Categoria c", Long.class);
        return query.getSingleResult();
    }
    
    /**
     * Remove categoria por ID
     */
    public void deleteById(UUID id) {
        boolean needsTransaction = !entityManager.getTransaction().isActive();
        if (needsTransaction) {
            entityManager.getTransaction().begin();
        }
        
        try {
            Categoria categoria = entityManager.find(Categoria.class, id);
            if (categoria != null) {
                entityManager.remove(categoria);
            }
            
            if (needsTransaction) {
                entityManager.getTransaction().commit();
            }
        } catch (Exception e) {
            if (needsTransaction && entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw e;
        }
    }
}