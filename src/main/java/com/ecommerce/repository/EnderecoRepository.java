package com.ecommerce.repository;

import com.ecommerce.domain.Cliente;
import com.ecommerce.domain.Endereco;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para operações com endereços
 */
public class EnderecoRepository {
    
    private final EntityManager entityManager;
    
    public EnderecoRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    
    /**
     * Salva ou atualiza um endereço
     */
    public Endereco save(Endereco endereco) {
        boolean needsTransaction = !entityManager.getTransaction().isActive();
        if (needsTransaction) {
            entityManager.getTransaction().begin();
        }
        
        try {
            if (endereco.getId() == null) {
                endereco.setId(UUID.randomUUID());
                entityManager.persist(endereco);
                entityManager.flush();
            } else {
                endereco = entityManager.merge(endereco);
                entityManager.flush();
            }
            
            if (needsTransaction) {
                entityManager.getTransaction().commit();
            }
            
            return endereco;
        } catch (Exception e) {
            if (needsTransaction && entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw e;
        }
    }
    
    /**
     * Busca endereço por ID
     */
    public Optional<Endereco> findById(UUID id) {
        Endereco endereco = entityManager.find(Endereco.class, id);
        return Optional.ofNullable(endereco);
    }
    
    /**
     * Busca endereço por ID e cliente ID
     * Usado para validar propriedade de endereço
     */
    public Optional<Endereco> findByIdAndClienteId(UUID id, UUID clienteId) {
        try {
            TypedQuery<Endereco> query = entityManager.createQuery(
                "SELECT e FROM Endereco e WHERE e.id = :id AND e.cliente.id = :clienteId", Endereco.class);
            query.setParameter("id", id);
            query.setParameter("clienteId", clienteId);
            Endereco endereco = query.getSingleResult();
            return Optional.of(endereco);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * Lista todos endereços por cliente ID
     * Alternativa mais explícita ao findByClienteId
     */
    public List<Endereco> findAllByClienteId(UUID clienteId) {
        TypedQuery<Endereco> query = entityManager.createQuery(
            "SELECT e FROM Endereco e WHERE e.cliente.id = :clienteId ORDER BY e.id", Endereco.class);
        query.setParameter("clienteId", clienteId);
        return query.getResultList();
    }
    
    /**
     * Lista endereços por cliente
     */
    public List<Endereco> findByCliente(Cliente cliente) {
        TypedQuery<Endereco> query = entityManager.createQuery(
            "SELECT e FROM Endereco e WHERE e.cliente = :cliente ORDER BY e.id", Endereco.class);
        query.setParameter("cliente", cliente);
        return query.getResultList();
    }
    
    /**
     * Lista endereços por cliente ID
     */
    public List<Endereco> findByClienteId(UUID clienteId) {
        TypedQuery<Endereco> query = entityManager.createQuery(
            "SELECT e FROM Endereco e WHERE e.cliente.id = :clienteId ORDER BY e.id", Endereco.class);
        query.setParameter("clienteId", clienteId);
        return query.getResultList();
    }
    
    /**
     * Busca endereço padrão por cliente ID
     */
    public Optional<Endereco> findByClienteIdAndEhPadrao(UUID clienteId, Boolean ehPadrao) {
        try {
            TypedQuery<Endereco> query = entityManager.createQuery(
                "SELECT e FROM Endereco e WHERE e.cliente.id = :clienteId AND e.ehPadrao = :ehPadrao", Endereco.class);
            query.setParameter("clienteId", clienteId);
            query.setParameter("ehPadrao", ehPadrao);
            Endereco endereco = query.getSingleResult();
            return Optional.of(endereco);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * Busca endereços por cidade
     */
    public List<Endereco> findByCidade(String cidade) {
        TypedQuery<Endereco> query = entityManager.createQuery(
            "SELECT e FROM Endereco e WHERE LOWER(e.cidade) = LOWER(:cidade) ORDER BY e.rua", Endereco.class);
        query.setParameter("cidade", cidade);
        return query.getResultList();
    }
    
    /**
     * Busca endereços por CEP
     */
    public List<Endereco> findByCep(String cep) {
        TypedQuery<Endereco> query = entityManager.createQuery(
            "SELECT e FROM Endereco e WHERE e.cep = :cep ORDER BY e.rua", Endereco.class);
        query.setParameter("cep", cep);
        return query.getResultList();
    }
    
    /**
     * Conta endereços por cliente
     */
    public long countByCliente(UUID clienteId) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(e) FROM Endereco e WHERE e.cliente.id = :clienteId", Long.class);
        query.setParameter("clienteId", clienteId);
        return query.getSingleResult();
    }
    
    /**
     * Verifica se cliente possui endereço específico
     */
    public boolean existsByClienteAndId(UUID clienteId, UUID enderecoId) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(e) FROM Endereco e WHERE e.cliente.id = :clienteId AND e.id = :enderecoId", Long.class);
        query.setParameter("clienteId", clienteId);
        query.setParameter("enderecoId", enderecoId);
        return query.getSingleResult() > 0;
    }
    
    /**
     * Desmarca endereço padrão de um cliente
     */
    public void desmarcarEnderecoPadrao(UUID clienteId) {
        boolean needsTransaction = !entityManager.getTransaction().isActive();
        if (needsTransaction) {
            entityManager.getTransaction().begin();
        }
        
        try {
            // Busca todos os endereços do cliente que estão marcados como padrão
            TypedQuery<Endereco> query = entityManager.createQuery(
                "SELECT e FROM Endereco e WHERE e.cliente.id = :clienteId AND e.ehPadrao = true", Endereco.class);
            query.setParameter("clienteId", clienteId);
            List<Endereco> enderecosPadrao = query.getResultList();
            
            // Atualiza cada endereço individualmente
            for (Endereco endereco : enderecosPadrao) {
                endereco.setEhPadrao(false);
                entityManager.merge(endereco);
            }
            
            // Força flush para garantir que as mudanças sejam aplicadas
            if (!enderecosPadrao.isEmpty()) {
                entityManager.flush();
                System.out.println("Desmarcados " + enderecosPadrao.size() + " endereços padrão para cliente: " + clienteId);
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
    
    /**
     * Remove endereço por ID
     */
    public void deleteById(UUID id) {
        Endereco endereco = entityManager.find(Endereco.class, id);
        if (endereco != null) {
            entityManager.remove(endereco);
        }
    }
    
    /**
     * Remove endereço
     */
    public void delete(Endereco endereco) {
        if (endereco != null && entityManager.contains(endereco)) {
            entityManager.remove(endereco);
        } else if (endereco != null && endereco.getId() != null) {
            Endereco managed = entityManager.find(Endereco.class, endereco.getId());
            if (managed != null) {
                entityManager.remove(managed);
            }
        }
    }
}