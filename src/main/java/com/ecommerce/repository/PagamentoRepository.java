package com.ecommerce.repository;

import com.ecommerce.domain.Pagamento;
import com.ecommerce.domain.PagamentoBoleto;
import com.ecommerce.domain.PagamentoCartao;
import com.ecommerce.domain.PagamentoPix;
import com.ecommerce.domain.Pedido;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para operações com pagamentos
 */
public class PagamentoRepository {
    
    private final EntityManager entityManager;
    
    public PagamentoRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    
    /**
     * Salva ou atualiza um pagamento
     */
    public Pagamento save(Pagamento pagamento) {
        if (pagamento.getId() == null) {
            pagamento.setId(UUID.randomUUID());
            entityManager.persist(pagamento);
            return pagamento;
        } else {
            return entityManager.merge(pagamento);
        }
    }
    
    /**
     * Busca pagamento por ID
     */
    public Optional<Pagamento> findById(UUID id) {
        Pagamento pagamento = entityManager.find(Pagamento.class, id);
        return Optional.ofNullable(pagamento);
    }
    
    /**
     * Busca pagamento por pedido
     */
    public Optional<Pagamento> findByPedido(Pedido pedido) {
        try {
            TypedQuery<Pagamento> query = entityManager.createQuery(
                "SELECT p FROM Pagamento p WHERE p.pedido = :pedido", Pagamento.class);
            query.setParameter("pedido", pedido);
            Pagamento pagamento = query.getSingleResult();
            return Optional.of(pagamento);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Busca pagamento por pedido ID
     */
    public Optional<Pagamento> findByPedidoId(UUID pedidoId) {
        try {
            TypedQuery<Pagamento> query = entityManager.createQuery(
                "SELECT p FROM Pagamento p WHERE p.pedido.id = :pedidoId", Pagamento.class);
            query.setParameter("pedidoId", pedidoId);
            Pagamento pagamento = query.getSingleResult();
            return Optional.of(pagamento);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Lista todos os pagamentos
     */
    public List<Pagamento> findAll() {
        TypedQuery<Pagamento> query = entityManager.createQuery(
            "SELECT p FROM Pagamento p ORDER BY p.id", Pagamento.class);
        return query.getResultList();
    }
    
    /**
     * Lista pagamentos por valor mínimo
     */
    public List<Pagamento> findByValorGreaterThan(BigDecimal valor) {
        TypedQuery<Pagamento> query = entityManager.createQuery(
            "SELECT p FROM Pagamento p WHERE p.valor > :valor ORDER BY p.valor DESC", Pagamento.class);
        query.setParameter("valor", valor);
        return query.getResultList();
    }
    
    /**
     * Conta total de pagamentos
     */
    public long count() {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(p) FROM Pagamento p", Long.class);
        return query.getSingleResult();
    }
    
    /**
     * Soma total de pagamentos
     */
    public BigDecimal sumTotal() {
        TypedQuery<BigDecimal> query = entityManager.createQuery(
            "SELECT COALESCE(SUM(p.valor), 0) FROM Pagamento p", BigDecimal.class);
        return query.getSingleResult();
    }
    
    // Métodos específicos para pagamentos PIX
    
    /**
     * Lista pagamentos PIX
     */
    public List<PagamentoPix> findAllPix() {
        TypedQuery<PagamentoPix> query = entityManager.createQuery(
            "SELECT p FROM PagamentoPix p ORDER BY p.id", PagamentoPix.class);
        return query.getResultList();
    }
    
    /**
     * Busca pagamento PIX por TXID
     */
    public Optional<PagamentoPix> findPixByTxid(String txid) {
        try {
            TypedQuery<PagamentoPix> query = entityManager.createQuery(
                "SELECT p FROM PagamentoPix p WHERE p.txid = :txid", PagamentoPix.class);
            query.setParameter("txid", txid);
            PagamentoPix pagamento = query.getSingleResult();
            return Optional.of(pagamento);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    // Métodos específicos para pagamentos Boleto
    
    /**
     * Lista pagamentos Boleto
     */
    public List<PagamentoBoleto> findAllBoleto() {
        TypedQuery<PagamentoBoleto> query = entityManager.createQuery(
            "SELECT p FROM PagamentoBoleto p ORDER BY p.id", PagamentoBoleto.class);
        return query.getResultList();
    }
    
    /**
     * Busca pagamento Boleto por linha digitável
     */
    public Optional<PagamentoBoleto> findBoletoByLinhaDigitavel(String linhaDigitavel) {
        try {
            TypedQuery<PagamentoBoleto> query = entityManager.createQuery(
                "SELECT p FROM PagamentoBoleto p WHERE p.linhaDigitavel = :linhaDigitavel", PagamentoBoleto.class);
            query.setParameter("linhaDigitavel", linhaDigitavel);
            PagamentoBoleto pagamento = query.getSingleResult();
            return Optional.of(pagamento);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    // Métodos específicos para pagamentos Cartão
    
    /**
     * Lista pagamentos Cartão
     */
    public List<PagamentoCartao> findAllCartao() {
        TypedQuery<PagamentoCartao> query = entityManager.createQuery(
            "SELECT p FROM PagamentoCartao p ORDER BY p.id", PagamentoCartao.class);
        return query.getResultList();
    }
    
    /**
     * Lista pagamentos por bandeira
     */
    public List<PagamentoCartao> findCartaoByBandeira(String bandeira) {
        TypedQuery<PagamentoCartao> query = entityManager.createQuery(
            "SELECT p FROM PagamentoCartao p WHERE p.bandeira = :bandeira ORDER BY p.id", PagamentoCartao.class);
        query.setParameter("bandeira", bandeira);
        return query.getResultList();
    }
    
    /**
     * Remove pagamento por ID
     */
    public void deleteById(UUID id) {
        Pagamento pagamento = entityManager.find(Pagamento.class, id);
        if (pagamento != null) {
            entityManager.remove(pagamento);
        }
    }
}