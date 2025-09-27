package com.ecommerce.repository;

import com.ecommerce.domain.Categoria;
import com.ecommerce.domain.Produto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para operações com produtos
 */
public class ProdutoRepository {
    
    private final EntityManager entityManager;
    
    public ProdutoRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    
    /**
     * Salva ou atualiza um produto
     */
    public Produto save(Produto produto) {
        boolean needsTransaction = !entityManager.getTransaction().isActive();
        if (needsTransaction) {
            entityManager.getTransaction().begin();
        }
        
        try {
            // Use merge for both new and existing entities - more flexible than persist
            produto = entityManager.merge(produto);
            entityManager.flush(); // Force INSERT/UPDATE to show in logs
            
            if (needsTransaction) {
                entityManager.getTransaction().commit();
            }
            
            return produto;
        } catch (Exception e) {
            if (needsTransaction && entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw e;
        }
    }
    
    /**
     * Busca produto por ID
     */
    public Optional<Produto> findById(UUID id) {
        Produto produto = entityManager.find(Produto.class, id);
        return Optional.ofNullable(produto);
    }
    
    /**
     * Busca produto por código de barras
     */
    public Optional<Produto> findByCodigoBarras(String codigoBarras) {
        try {
            TypedQuery<Produto> query = entityManager.createQuery(
                "SELECT p FROM Produto p WHERE p.codigoBarras = :codigoBarras", Produto.class);
            query.setParameter("codigoBarras", codigoBarras);
            Produto produto = query.getSingleResult();
            return Optional.of(produto);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Lista produtos por categoria
     */
    public List<Produto> findByCategoria(Categoria categoria) {
        TypedQuery<Produto> query = entityManager.createQuery(
            "SELECT p FROM Produto p WHERE p.categoria = :categoria ORDER BY p.nome", Produto.class);
        query.setParameter("categoria", categoria);
        return query.getResultList();
    }
    
    /**
     * Lista produtos por categoria ID
     */
    public List<Produto> findByCategoriaId(UUID categoriaId) {
        TypedQuery<Produto> query = entityManager.createQuery(
            "SELECT p FROM Produto p WHERE p.categoria.id = :categoriaId ORDER BY p.nome", Produto.class);
        query.setParameter("categoriaId", categoriaId);
        return query.getResultList();
    }
    
    /**
     * Busca produtos por nome (busca parcial)
     */
    public List<Produto> findByNomeContaining(String nome) {
        TypedQuery<Produto> query = entityManager.createQuery(
            "SELECT p FROM Produto p WHERE LOWER(p.nome) LIKE LOWER(:nome) ORDER BY p.nome", Produto.class);
        query.setParameter("nome", "%" + nome + "%");
        return query.getResultList();
    }
    
    /**
     * Lista produtos por faixa de preço
     */
    public List<Produto> findByPrecoRange(BigDecimal precoMin, BigDecimal precoMax) {
        TypedQuery<Produto> query = entityManager.createQuery(
            "SELECT p FROM Produto p WHERE p.preco BETWEEN :precoMin AND :precoMax ORDER BY p.preco", Produto.class);
        query.setParameter("precoMin", precoMin);
        query.setParameter("precoMax", precoMax);
        return query.getResultList();
    }
    
    /**
     * Lista todos os produtos
     */
    public List<Produto> findAll() {
        TypedQuery<Produto> query = entityManager.createQuery(
            "SELECT p FROM Produto p ORDER BY p.nome", Produto.class);
        return query.getResultList();
    }
    
    /**
     * Verifica se existe produto com código de barras
     */
    public boolean existsByCodigoBarras(String codigoBarras) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(p) FROM Produto p WHERE p.codigoBarras = :codigoBarras", Long.class);
        query.setParameter("codigoBarras", codigoBarras);
        return query.getSingleResult() > 0;
    }
    
    /**
     * Conta produtos por categoria
     */
    public long countByCategoria(UUID categoriaId) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(p) FROM Produto p WHERE p.categoria.id = :categoriaId", Long.class);
        query.setParameter("categoriaId", categoriaId);
        return query.getSingleResult();
    }
    
    /**
     * Remove produto por ID
     */
    public void deleteById(UUID id) {
        Produto produto = entityManager.find(Produto.class, id);
        if (produto != null) {
            entityManager.remove(produto);
        }
    }
    
    public long count() {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(p) FROM Produto p", Long.class);
        return query.getSingleResult();
    }
}