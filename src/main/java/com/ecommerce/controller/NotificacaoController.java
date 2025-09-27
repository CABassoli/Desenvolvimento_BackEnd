package com.ecommerce.controller;

import com.ecommerce.domain.Notificacao;
import com.ecommerce.dto.response.NotificacaoResponseDTO;
import com.ecommerce.service.NotificacaoService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Controller para consulta de notificações
 */
public class NotificacaoController {
    
    private final NotificacaoService notificacaoService;
    
    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }
    
    /**
     * GET /notificacoes - Listar todas as notificações
     */
    public void findAll(Context ctx) {
        try {
            var notificacoes = notificacaoService.findAll();
            
            ctx.status(HttpStatus.OK);
            ctx.json(notificacoes);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar notificações", e.getMessage()));
        }
    }
    
    /**
     * GET /notificacoes/{id} - Buscar notificação por ID
     */
    public void findById(Context ctx) {
        try {
            UUID id = UUID.fromString(ctx.pathParam("id"));
            
            var notificacaoOpt = notificacaoService.findById(id);
            if (notificacaoOpt.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(new AuthController.ErrorResponse("Notificação não encontrada", "ID: " + id));
                return;
            }
            
            ctx.status(HttpStatus.OK);
            ctx.json(notificacaoOpt.get());
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("ID inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar notificação", e.getMessage()));
        }
    }
    
    /**
     * GET /notificacoes/cliente/{clienteId} - Listar notificações do cliente
     */
    public void findByCliente(Context ctx) {
        try {
            UUID clienteId = UUID.fromString(ctx.pathParam("clienteId"));
            
            var notificacoes = notificacaoService.findByCliente(clienteId);
            
            ctx.status(HttpStatus.OK);
            ctx.json(notificacoes);
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("ID de cliente inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar notificações", e.getMessage()));
        }
    }
    
    /**
     * GET /notificacoes/cliente/{clienteId}/recentes - Notificações recentes do cliente
     * Query param: limit (default: 10)
     */
    public void findRecentByCliente(Context ctx) {
        try {
            UUID clienteId = UUID.fromString(ctx.pathParam("clienteId"));
            
            String limitStr = ctx.queryParam("limit");
            int limit = limitStr != null ? Integer.parseInt(limitStr) : 10;
            
            if (limit <= 0 || limit > 100) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new AuthController.ErrorResponse("Limite inválido", "Limite deve ser entre 1 e 100"));
                return;
            }
            
            var notificacoes = notificacaoService.findRecentByCliente(clienteId, limit);
            
            ctx.status(HttpStatus.OK);
            ctx.json(notificacoes);
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("Parâmetros inválidos", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar notificações", e.getMessage()));
        }
    }
    
    /**
     * GET /notificacoes/pedido/{pedidoId} - Listar notificações do pedido
     */
    public void findByPedido(Context ctx) {
        try {
            UUID pedidoId = UUID.fromString(ctx.pathParam("pedidoId"));
            
            var notificacoes = notificacaoService.findByPedido(pedidoId);
            
            ctx.status(HttpStatus.OK);
            ctx.json(notificacoes);
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("ID de pedido inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar notificações", e.getMessage()));
        }
    }
    
    /**
     * GET /notificacoes/tipo/{tipo} - Listar notificações por tipo
     */
    public void findByTipo(Context ctx) {
        try {
            String tipoStr = ctx.pathParam("tipo").toUpperCase();
            Notificacao.TipoNotificacao tipo = Notificacao.TipoNotificacao.valueOf(tipoStr);
            
            var notificacoes = notificacaoService.findByTipo(tipo);
            
            ctx.status(HttpStatus.OK);
            ctx.json(notificacoes);
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("Tipo inválido", 
                "Tipos válidos: CONFIRMACAO, STATUS"));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar notificações", e.getMessage()));
        }
    }
    
    /**
     * GET /notificacoes/periodo - Listar notificações entre datas
     * Query params: inicio, fim (formato: yyyy-MM-dd'T'HH:mm:ss)
     */
    public void findByPeriodo(Context ctx) {
        try {
            String inicioStr = ctx.queryParam("inicio");
            String fimStr = ctx.queryParam("fim");
            
            if (inicioStr == null || fimStr == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new AuthController.ErrorResponse("Parâmetros obrigatórios", 
                    "inicio e fim são obrigatórios (formato: yyyy-MM-dd'T'HH:mm:ss)"));
                return;
            }
            
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            LocalDateTime inicio = LocalDateTime.parse(inicioStr, formatter);
            LocalDateTime fim = LocalDateTime.parse(fimStr, formatter);
            
            var notificacoes = notificacaoService.findByPeriodo(inicio, fim);
            
            ctx.status(HttpStatus.OK);
            ctx.json(notificacoes);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("Erro nos parâmetros de data", e.getMessage()));
        }
    }
    
    /**
     * GET /notificacoes/count/cliente/{clienteId} - Contar notificações do cliente
     */
    public void countByCliente(Context ctx) {
        try {
            UUID clienteId = UUID.fromString(ctx.pathParam("clienteId"));
            
            long count = notificacaoService.countByCliente(clienteId);
            
            ctx.status(HttpStatus.OK);
            ctx.json(new CategoriaController.CountResponse(count));
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("ID de cliente inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao contar notificações", e.getMessage()));
        }
    }
    
    /**
     * GET /notificacoes/count/tipo/{tipo} - Contar notificações por tipo
     */
    public void countByTipo(Context ctx) {
        try {
            String tipoStr = ctx.pathParam("tipo").toUpperCase();
            Notificacao.TipoNotificacao tipo = Notificacao.TipoNotificacao.valueOf(tipoStr);
            
            long count = notificacaoService.countByTipo(tipo);
            
            ctx.status(HttpStatus.OK);
            ctx.json(new CategoriaController.CountResponse(count));
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("Tipo inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao contar notificações", e.getMessage()));
        }
    }
    
    /**
     * DELETE /notificacoes/cleanup/{days} - Remover notificações antigas
     */
    public void removeOldNotifications(Context ctx) {
        try {
            int days = Integer.parseInt(ctx.pathParam("days"));
            
            if (days <= 0 || days > 365) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new AuthController.ErrorResponse("Dias inválido", "Deve ser entre 1 e 365"));
                return;
            }
            
            notificacaoService.removeOldNotifications(days);
            
            ctx.status(HttpStatus.OK);
            ctx.json(new AuthController.SuccessResponse("Notificações antigas removidas"));
            
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("Número de dias inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao remover notificações", e.getMessage()));
        }
    }
}