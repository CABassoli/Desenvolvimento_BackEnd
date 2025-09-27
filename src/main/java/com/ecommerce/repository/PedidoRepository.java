package com.ecommerce.repository;

import com.ecommerce.domain.Cliente;
import com.ecommerce.domain.Pedido;
import com.ecommerce.domain.StatusPedido;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para operações com pedidos
 */
public class PedidoRepository {
    
    // Não armazena mais EntityManager fixo - usa o do request atual
    private final EntityManager defaultEntityManager;
    
    public PedidoRepository(EntityManager defaultEntityManager) {
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
            // Se não houver request ativo, usa o default (para testes ou operações fora de request)
            return defaultEntityManager;
        }
    }
    
    /**
     * Salva ou atualiza um pedido (transação gerenciada pelo TransactionFilter)
     */
    public Pedido save(Pedido pedido) {
        EntityManager em = getEntityManager();
        try {
            if (pedido.getId() == null) {
                // NÃO definir ID manualmente - deixar @GeneratedValue gerar
                em.persist(pedido);
            } else {
                pedido = em.merge(pedido);
            }
            
            // Garantir que as mudanças sejam refletidas imediatamente
            em.flush();
            
            System.out.println("✅ PEDIDO: Pedido salvo com ID: " + pedido.getId() + " - Valor: " + pedido.getValorTotal());
            return pedido;
        } catch (Exception e) {
            System.err.println("❌ PEDIDO: Erro ao salvar pedido: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erro ao salvar pedido: " + e.getMessage(), e);
        }
    }
    
    /**
     * Busca pedido por ID
     */
    public Optional<Pedido> findById(UUID id) {
        Pedido pedido = getEntityManager().find(Pedido.class, id);
        return Optional.ofNullable(pedido);
    }
    
    /**
     * Busca pedido com itens carregados
     */
    public Optional<Pedido> findByIdWithItens(UUID id) {
        try {
            TypedQuery<Pedido> query = getEntityManager().createQuery(
                "SELECT p FROM Pedido p LEFT JOIN FETCH p.itens WHERE p.id = :id", Pedido.class);
            query.setParameter("id", id);
            Pedido pedido = query.getSingleResult();
            return Optional.of(pedido);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * Busca pedido por chave de idempotência
     */
    public Optional<Pedido> findByIdempotencyKey(String idempotencyKey) {
        try {
            TypedQuery<Pedido> query = getEntityManager().createQuery(
                "SELECT p FROM Pedido p WHERE p.idempotencyKey = :idempotencyKey", Pedido.class);
            query.setParameter("idempotencyKey", idempotencyKey);
            Pedido pedido = query.getSingleResult();
            return Optional.of(pedido);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * Lista pedidos por cliente
     */
    public List<Pedido> findByCliente(Cliente cliente) {
        TypedQuery<Pedido> query = getEntityManager().createQuery(
            "SELECT p FROM Pedido p WHERE p.cliente = :cliente ORDER BY p.dataPedido DESC", Pedido.class);
        query.setParameter("cliente", cliente);
        return query.getResultList();
    }
    
    /**
     * Lista pedidos por cliente ID
     */
    public List<Pedido> findByClienteId(UUID clienteId) {
        TypedQuery<Pedido> query = getEntityManager().createQuery(
            "SELECT p FROM Pedido p WHERE p.cliente.id = :clienteId ORDER BY p.dataPedido DESC", Pedido.class);
        query.setParameter("clienteId", clienteId);
        return query.getResultList();
    }
    
    /**
     * Lista pedidos por status
     */
    public List<Pedido> findByStatus(StatusPedido status) {
        TypedQuery<Pedido> query = getEntityManager().createQuery(
            "SELECT p FROM Pedido p WHERE p.status = :status ORDER BY p.dataPedido DESC", Pedido.class);
        query.setParameter("status", status);
        return query.getResultList();
    }
    
    /**
     * Lista pedidos por período
     */
    public List<Pedido> findByDataPedidoBetween(LocalDateTime inicio, LocalDateTime fim) {
        TypedQuery<Pedido> query = getEntityManager().createQuery(
            "SELECT p FROM Pedido p WHERE p.dataPedido BETWEEN :inicio AND :fim ORDER BY p.dataPedido DESC", Pedido.class);
        query.setParameter("inicio", inicio);
        query.setParameter("fim", fim);
        return query.getResultList();
    }
    
    /**
     * Lista pedidos por cliente e status
     */
    public List<Pedido> findByClienteIdAndStatus(UUID clienteId, StatusPedido status) {
        TypedQuery<Pedido> query = getEntityManager().createQuery(
            "SELECT p FROM Pedido p WHERE p.cliente.id = :clienteId AND p.status = :status ORDER BY p.dataPedido DESC", Pedido.class);
        query.setParameter("clienteId", clienteId);
        query.setParameter("status", status);
        return query.getResultList();
    }
    
    /**
     * Lista todos os pedidos
     */
    public List<Pedido> findAll() {
        TypedQuery<Pedido> query = getEntityManager().createQuery(
            "SELECT p FROM Pedido p ORDER BY p.dataPedido DESC", Pedido.class);
        return query.getResultList();
    }
    
    public List<Pedido> findByClienteIdOrderByCreatedAtDesc(UUID clienteId) {
        TypedQuery<Pedido> query = getEntityManager().createQuery(
            "SELECT p FROM Pedido p WHERE p.cliente.id = :clienteId ORDER BY COALESCE(p.createdAt, p.dataPedido) DESC", Pedido.class);
        query.setParameter("clienteId", clienteId);
        return query.getResultList();
    }
    
    public List<Pedido> findAllByOrderByCreatedAtDesc() {
        TypedQuery<Pedido> query = getEntityManager().createQuery(
            "SELECT p FROM Pedido p ORDER BY COALESCE(p.createdAt, p.dataPedido) DESC", Pedido.class);
        return query.getResultList();
    }
    
    public long countDistinctClienteId() {
        TypedQuery<Long> query = getEntityManager().createQuery(
            "SELECT COUNT(DISTINCT p.cliente.id) FROM Pedido p", Long.class);
        return query.getSingleResult();
    }
    
    /**
     * Conta pedidos por status
     */
    public long countByStatus(StatusPedido status) {
        TypedQuery<Long> query = getEntityManager().createQuery(
            "SELECT COUNT(p) FROM Pedido p WHERE p.status = :status", Long.class);
        query.setParameter("status", status);
        return query.getSingleResult();
    }
    
    /**
     * Conta pedidos por cliente
     */
    public long countByCliente(UUID clienteId) {
        TypedQuery<Long> query = getEntityManager().createQuery(
            "SELECT COUNT(p) FROM Pedido p WHERE p.cliente.id = :clienteId", Long.class);
        query.setParameter("clienteId", clienteId);
        return query.getSingleResult();
    }
    
    /**
     * Conta total de pedidos
     */
    public long count() {
        TypedQuery<Long> query = getEntityManager().createQuery(
            "SELECT COUNT(p) FROM Pedido p", Long.class);
        return query.getSingleResult();
    }
    
    /**
     * Remove pedido por ID
     */
    public void deleteById(UUID id) {
        EntityManager em = getEntityManager();
        Pedido pedido = em.find(Pedido.class, id);
        if (pedido != null) {
            em.remove(pedido);
        }
    }
}