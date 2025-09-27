package com.ecommerce.controller;

import com.ecommerce.domain.StatusPedido;
import com.ecommerce.service.PedidoService;
import com.ecommerce.service.ProdutoService;
import com.ecommerce.service.ClienteService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AdminController {
    
    private final PedidoService pedidoService;
    private final ProdutoService produtoService;
    private final ClienteService clienteService;
    
    public AdminController(PedidoService pedidoService, ProdutoService produtoService, ClienteService clienteService) {
        this.pedidoService = pedidoService;
        this.produtoService = produtoService;
        this.clienteService = clienteService;
    }
    
    public void getMetricas(Context ctx) {
        try {
            String userRole = ctx.attribute("userRole");
            
            if (!"MANAGER".equals(userRole)) {
                ctx.status(HttpStatus.FORBIDDEN);
                ctx.json(new AuthController.ErrorResponse("Acesso negado", "Apenas administradores podem acessar"));
                return;
            }
            
            Map<String, Object> metricas = new HashMap<>();
            
            long totalProdutos = produtoService.count();
            metricas.put("totalProdutos", totalProdutos);
            
            long totalPedidos = pedidoService.countPedidos();
            metricas.put("totalPedidos", totalPedidos);
            
            long totalClientes = pedidoService.countClientesPagos();
            metricas.put("totalClientes", totalClientes);
            
            BigDecimal faturamento = pedidoService.getTotalFaturamento();
            metricas.put("faturamento", faturamento != null ? faturamento : BigDecimal.ZERO);
            
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.status(HttpStatus.OK);
            ctx.json(metricas);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar métricas", e.getMessage()));
        }
    }
    
    /**
     * PATCH /api/admin/pedidos/{id}/status - Atualizar status do pedido (ENVIADO ou ENTREGUE)
     */
    public void updateOrderStatus(Context ctx) {
        try {
            String userRole = ctx.attribute("userRole");
            
            if (!"MANAGER".equals(userRole)) {
                ctx.status(HttpStatus.FORBIDDEN);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Acesso negado", "Apenas administradores podem acessar"));
                return;
            }
            
            UUID pedidoId = UUID.fromString(ctx.pathParam("id"));
            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String statusStr = body.get("status");
            
            if (statusStr == null || statusStr.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Erro de validação", "Status é obrigatório"));
                return;
            }
            
            StatusPedido novoStatus;
            try {
                novoStatus = StatusPedido.valueOf(statusStr.toUpperCase());
                // Validar se o status é permitido para admin (ENVIADO ou ENTREGUE)
                if (novoStatus != StatusPedido.ENVIADO && novoStatus != StatusPedido.ENTREGUE) {
                    ctx.status(HttpStatus.BAD_REQUEST);
                    ctx.header("Cache-Control", "no-store");
                    ctx.json(new AuthController.ErrorResponse("Erro de validação", 
                        "Administrador pode apenas atualizar para ENVIADO ou ENTREGUE"));
                    return;
                }
            } catch (IllegalArgumentException e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Erro de validação", 
                    "Status inválido. Use: ENVIADO ou ENTREGUE"));
                return;
            }
            
            var pedidoAtualizado = pedidoService.atualizarStatus(pedidoId, novoStatus);
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store");
            ctx.json(pedidoAtualizado);
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("não encontrado")) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Não encontrado", e.getMessage()));
            } else if (e.getMessage().contains("não é permitida")) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.header("Cache-Control", "no-store");
                Map<String, String> error = new HashMap<>();
                error.put("error", "transicao_invalida");
                error.put("message", e.getMessage());
                ctx.json(error);
            } else {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Erro ao atualizar status", e.getMessage()));
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.header("Cache-Control", "no-store");
            ctx.json(new AuthController.ErrorResponse("Erro ao atualizar status", e.getMessage()));
        }
    }
}