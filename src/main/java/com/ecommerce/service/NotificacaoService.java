package com.ecommerce.service;

import com.ecommerce.domain.Cliente;
import com.ecommerce.domain.Notificacao;
import com.ecommerce.domain.Pedido;
import com.ecommerce.domain.StatusPedido;
import com.ecommerce.dto.response.NotificacaoResponseDTO;
import com.ecommerce.mapper.NotificacaoMapper;
import com.ecommerce.repository.NotificacaoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço para gerenciamento de notificações
 * Responsável por criar e gerenciar notificações de pedidos
 */
public class NotificacaoService {
    
    private final NotificacaoRepository notificacaoRepository;
    private final NotificacaoMapper notificacaoMapper;
    
    public NotificacaoService(NotificacaoRepository notificacaoRepository,
                             NotificacaoMapper notificacaoMapper) {
        this.notificacaoRepository = notificacaoRepository;
        this.notificacaoMapper = notificacaoMapper;
    }
    
    /**
     * Cria notificação de confirmação de pedido
     * 
     * @param cliente O cliente do pedido
     * @param pedido O pedido confirmado
     * @return A notificação criada
     */
    public NotificacaoResponseDTO criarNotificacaoConfirmacao(Cliente cliente, Pedido pedido) {
        String mensagem = String.format(
            "Seu pedido #%s foi confirmado com sucesso! Valor total: R$ %.2f",
            pedido.getId().toString().substring(0, 8),
            pedido.getValorTotal()
        );
        
        Notificacao notificacao = new Notificacao();
        notificacao.setId(UUID.randomUUID());
        notificacao.setCliente(cliente);
        notificacao.setPedido(pedido);
        notificacao.setTipo(Notificacao.TipoNotificacao.CONFIRMACAO);
        notificacao.setMensagem(mensagem);
        notificacao.setCriadoEm(LocalDateTime.now());
        
        Notificacao savedNotificacao = notificacaoRepository.save(notificacao);
        
        return notificacaoMapper.toResponseDTO(savedNotificacao);
    }
    
    /**
     * Cria notificação de mudança de status do pedido
     * 
     * @param cliente O cliente do pedido
     * @param pedido O pedido atualizado
     * @param novoStatus O novo status do pedido
     * @return A notificação criada
     */
    public NotificacaoResponseDTO criarNotificacaoStatus(Cliente cliente, Pedido pedido, StatusPedido novoStatus) {
        String mensagem = gerarMensagemStatus(pedido, novoStatus);
        
        Notificacao notificacao = new Notificacao();
        notificacao.setId(UUID.randomUUID());
        notificacao.setCliente(cliente);
        notificacao.setPedido(pedido);
        notificacao.setTipo(Notificacao.TipoNotificacao.STATUS);
        notificacao.setMensagem(mensagem);
        notificacao.setCriadoEm(LocalDateTime.now());
        
        Notificacao savedNotificacao = notificacaoRepository.save(notificacao);
        
        return notificacaoMapper.toResponseDTO(savedNotificacao);
    }
    
    /**
     * Lista notificações do cliente (mais recentes primeiro)
     * 
     * @param clienteId O ID do cliente
     * @return Lista de notificações ordenadas
     */
    public List<NotificacaoResponseDTO> findByCliente(UUID clienteId) {
        return notificacaoRepository.findByClienteId(clienteId).stream()
                .map(notificacaoMapper::toResponseDTO)
                .toList();
    }
    
    /**
     * Lista notificações recentes do cliente com limite
     * 
     * @param clienteId O ID do cliente
     * @param limit Número máximo de notificações
     * @return Lista limitada de notificações
     */
    public List<NotificacaoResponseDTO> findRecentByCliente(UUID clienteId, int limit) {
        return notificacaoRepository.findRecentByClienteId(clienteId, limit).stream()
                .map(notificacaoMapper::toResponseDTO)
                .toList();
    }
    
    /**
     * Lista notificações de um pedido específico
     * 
     * @param pedidoId O ID do pedido
     * @return Lista de notificações do pedido
     */
    public List<NotificacaoResponseDTO> findByPedido(UUID pedidoId) {
        return notificacaoRepository.findByPedidoId(pedidoId).stream()
                .map(notificacaoMapper::toResponseDTO)
                .toList();
    }
    
    /**
     * Lista notificações filtradas por tipo
     * 
     * @param tipo O tipo de notificação
     * @return Lista de notificações do tipo especificado
     */
    public List<NotificacaoResponseDTO> findByTipo(Notificacao.TipoNotificacao tipo) {
        return notificacaoRepository.findByTipo(tipo).stream()
                .map(notificacaoMapper::toResponseDTO)
                .toList();
    }
    
    /**
     * Lista notificações criadas em um período
     * 
     * @param inicio Data/hora inicial
     * @param fim Data/hora final
     * @return Lista de notificações no período
     */
    public List<NotificacaoResponseDTO> findByPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return notificacaoRepository.findByCriadoEmBetween(inicio, fim).stream()
                .map(notificacaoMapper::toResponseDTO)
                .toList();
    }
    
    /**
     * Busca notificação por ID
     * 
     * @param id O ID da notificação
     * @return A notificação se encontrada
     */
    public Optional<NotificacaoResponseDTO> findById(UUID id) {
        return notificacaoRepository.findById(id)
                .map(notificacaoMapper::toResponseDTO);
    }
    
    /**
     * Lista todas as notificações do sistema (para administradores)
     * 
     * @return Lista completa de notificações
     */
    public List<NotificacaoResponseDTO> findAll() {
        return notificacaoRepository.findAll().stream()
                .map(notificacaoMapper::toResponseDTO)
                .toList();
    }
    
    /**
     * Conta o total de notificações de um cliente
     * 
     * @param clienteId O ID do cliente
     * @return Número de notificações
     */
    public long countByCliente(UUID clienteId) {
        return notificacaoRepository.countByCliente(clienteId);
    }
    
    /**
     * Conta notificações por tipo
     * 
     * @param tipo O tipo de notificação
     * @return Número de notificações do tipo
     */
    public long countByTipo(Notificacao.TipoNotificacao tipo) {
        return notificacaoRepository.countByTipo(tipo);
    }
    
    /**
     * Remove notificações antigas para limpeza automática
     * 
     * @param daysOld Número de dias de idade para exclusão
     */
    public void removeOldNotifications(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        notificacaoRepository.deleteOlderThan(cutoffDate);
    }
    
    /**
     * Gera mensagem personalizada baseada no status do pedido
     * 
     * @param pedido O pedido
     * @param status O status do pedido
     * @return Mensagem formatada para o cliente
     */
    private String gerarMensagemStatus(Pedido pedido, StatusPedido status) {
        String pedidoId = pedido.getId().toString().substring(0, 8);
        
        return switch (status) {
            case NOVO -> String.format("Seu pedido #%s foi recebido e está aguardando confirmação.", pedidoId);
            case PROCESSANDO -> String.format("Seu pedido #%s está sendo processado.", pedidoId);
            case PAGO -> String.format("Pagamento confirmado! Seu pedido #%s foi aprovado com sucesso.", pedidoId);
            case ENVIADO -> String.format("Seu pedido #%s foi enviado e está a caminho!", pedidoId);
            case ENTREGUE -> String.format("Seu pedido #%s foi entregue com sucesso!", pedidoId);
            case CANCELADO -> String.format("Seu pedido #%s foi cancelado conforme solicitado.", pedidoId);
        };
    }
}