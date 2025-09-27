package com.ecommerce.repository;

import com.ecommerce.domain.Cliente;
import com.ecommerce.domain.Notificacao;
import com.ecommerce.domain.Pedido;
import com.ecommerce.domain.Notificacao.TipoNotificacao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para operações com notificações
 */
public class NotificacaoRepository {
    
    private final EntityManager entityManager;
    
    public NotificacaoRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    
    /**
     * Salva ou atualiza uma notificação
     */
    public Notificacao save(Notificacao notificacao) {
        if (notificacao.getId() == null) {
            notificacao.setId(UUID.randomUUID());
            entityManager.persist(notificacao);
            return notificacao;
        } else {
            return entityManager.merge(notificacao);
        }
    }
    
    /**
     * Busca notificação por ID
     */
    public Optional<Notificacao> findById(UUID id) {
        Notificacao notificacao = entityManager.find(Notificacao.class, id);
        return Optional.ofNullable(notificacao);
    }
    
    /**
     * Lista notificações por cliente
     */
    public List<Notificacao> findByCliente(Cliente cliente) {
        TypedQuery<Notificacao> query = entityManager.createQuery(
            "SELECT n FROM Notificacao n WHERE n.cliente = :cliente ORDER BY n.criadoEm DESC", Notificacao.class);
        query.setParameter("cliente", cliente);
        return query.getResultList();
    }
    
    /**
     * Lista notificações por cliente ID
     */
    public List<Notificacao> findByClienteId(UUID clienteId) {
        TypedQuery<Notificacao> query = entityManager.createQuery(
            "SELECT n FROM Notificacao n WHERE n.cliente.id = :clienteId ORDER BY n.criadoEm DESC", Notificacao.class);
        query.setParameter("clienteId", clienteId);
        return query.getResultList();
    }
    
    /**
     * Lista notificações por pedido
     */
    public List<Notificacao> findByPedido(Pedido pedido) {
        TypedQuery<Notificacao> query = entityManager.createQuery(
            "SELECT n FROM Notificacao n WHERE n.pedido = :pedido ORDER BY n.criadoEm DESC", Notificacao.class);
        query.setParameter("pedido", pedido);
        return query.getResultList();
    }
    
    /**
     * Lista notificações por pedido ID
     */
    public List<Notificacao> findByPedidoId(UUID pedidoId) {
        TypedQuery<Notificacao> query = entityManager.createQuery(
            "SELECT n FROM Notificacao n WHERE n.pedido.id = :pedidoId ORDER BY n.criadoEm DESC", Notificacao.class);
        query.setParameter("pedidoId", pedidoId);
        return query.getResultList();
    }
    
    /**
     * Lista notificações por tipo
     */
    public List<Notificacao> findByTipo(TipoNotificacao tipo) {
        TypedQuery<Notificacao> query = entityManager.createQuery(
            "SELECT n FROM Notificacao n WHERE n.tipo = :tipo ORDER BY n.criadoEm DESC", Notificacao.class);
        query.setParameter("tipo", tipo);
        return query.getResultList();
    }
    
    /**
     * Lista notificações por cliente e tipo
     */
    public List<Notificacao> findByClienteIdAndTipo(UUID clienteId, TipoNotificacao tipo) {
        TypedQuery<Notificacao> query = entityManager.createQuery(
            "SELECT n FROM Notificacao n WHERE n.cliente.id = :clienteId AND n.tipo = :tipo ORDER BY n.criadoEm DESC", Notificacao.class);
        query.setParameter("clienteId", clienteId);
        query.setParameter("tipo", tipo);
        return query.getResultList();
    }
    
    /**
     * Lista notificações por período
     */
    public List<Notificacao> findByCriadoEmBetween(LocalDateTime inicio, LocalDateTime fim) {
        TypedQuery<Notificacao> query = entityManager.createQuery(
            "SELECT n FROM Notificacao n WHERE n.criadoEm BETWEEN :inicio AND :fim ORDER BY n.criadoEm DESC", Notificacao.class);
        query.setParameter("inicio", inicio);
        query.setParameter("fim", fim);
        return query.getResultList();
    }
    
    /**
     * Lista notificações recentes por cliente (últimas 30)
     */
    public List<Notificacao> findRecentByClienteId(UUID clienteId, int limit) {
        TypedQuery<Notificacao> query = entityManager.createQuery(
            "SELECT n FROM Notificacao n WHERE n.cliente.id = :clienteId ORDER BY n.criadoEm DESC", Notificacao.class);
        query.setParameter("clienteId", clienteId);
        query.setMaxResults(limit);
        return query.getResultList();
    }
    
    /**
     * Lista todas as notificações
     */
    public List<Notificacao> findAll() {
        TypedQuery<Notificacao> query = entityManager.createQuery(
            "SELECT n FROM Notificacao n ORDER BY n.criadoEm DESC", Notificacao.class);
        return query.getResultList();
    }
    
    /**
     * Conta notificações por cliente
     */
    public long countByCliente(UUID clienteId) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(n) FROM Notificacao n WHERE n.cliente.id = :clienteId", Long.class);
        query.setParameter("clienteId", clienteId);
        return query.getSingleResult();
    }
    
    /**
     * Conta notificações por tipo
     */
    public long countByTipo(TipoNotificacao tipo) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(n) FROM Notificacao n WHERE n.tipo = :tipo", Long.class);
        query.setParameter("tipo", tipo);
        return query.getSingleResult();
    }
    
    /**
     * Conta total de notificações
     */
    public long count() {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(n) FROM Notificacao n", Long.class);
        return query.getSingleResult();
    }
    
    /**
     * Remove notificação por ID
     */
    public void deleteById(UUID id) {
        Notificacao notificacao = entityManager.find(Notificacao.class, id);
        if (notificacao != null) {
            entityManager.remove(notificacao);
        }
    }
    
    /**
     * Remove notificações antigas (mais de X dias)
     */
    public void deleteOlderThan(LocalDateTime cutoffDate) {
        TypedQuery<Notificacao> query = entityManager.createQuery(
            "SELECT n FROM Notificacao n WHERE n.criadoEm < :cutoffDate", Notificacao.class);
        query.setParameter("cutoffDate", cutoffDate);
        List<Notificacao> notificacoes = query.getResultList();
        for (Notificacao notificacao : notificacoes) {
            entityManager.remove(notificacao);
        }
    }
}