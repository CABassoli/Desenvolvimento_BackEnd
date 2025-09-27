package com.ecommerce.repository;

import com.ecommerce.domain.UserModel;
import com.ecommerce.domain.Role;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para operações com usuários
 */
public class UserRepository {
    
    private final EntityManager entityManager;
    
    public UserRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    
    /**
     * Salva ou atualiza um usuário
     */
    public UserModel save(UserModel user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
            entityManager.persist(user);
            return user;
        } else {
            return entityManager.merge(user);
        }
    }
    
    /**
     * Busca usuário por ID
     */
    public Optional<UserModel> findById(UUID id) {
        UserModel user = entityManager.find(UserModel.class, id);
        return Optional.ofNullable(user);
    }
    
    /**
     * Busca usuário por email
     */
    public Optional<UserModel> findByEmail(String email) {
        try {
            TypedQuery<UserModel> query = entityManager.createQuery(
                "SELECT u FROM UserModel u WHERE u.email = :email", UserModel.class);
            query.setParameter("email", email);
            UserModel user = query.getSingleResult();
            return Optional.of(user);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Busca usuários ativos por role
     */
    public List<UserModel> findActiveByRole(Role role) {
        TypedQuery<UserModel> query = entityManager.createQuery(
            "SELECT u FROM UserModel u WHERE u.role = :role AND u.isActive = true", UserModel.class);
        query.setParameter("role", role);
        return query.getResultList();
    }
    
    /**
     * Verifica se existe usuário com email
     */
    public boolean existsByEmail(String email) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(u) FROM UserModel u WHERE u.email = :email", Long.class);
        query.setParameter("email", email);
        return query.getSingleResult() > 0;
    }
    
    /**
     * Lista todos os usuários ativos
     */
    public List<UserModel> findAllActive() {
        TypedQuery<UserModel> query = entityManager.createQuery(
            "SELECT u FROM UserModel u WHERE u.isActive = true ORDER BY u.createdAt DESC", UserModel.class);
        return query.getResultList();
    }
    
    /**
     * Remove usuário por ID
     */
    public void deleteById(UUID id) {
        UserModel user = entityManager.find(UserModel.class, id);
        if (user != null) {
            entityManager.remove(user);
        }
    }
}