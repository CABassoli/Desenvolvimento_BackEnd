package com.ecommerce.controller;

import com.ecommerce.domain.StatusPedido;
import com.ecommerce.dto.ConfirmarPedidoRequestDTO;
import com.ecommerce.dto.request.PedidoRequestDTO;
import com.ecommerce.dto.response.PedidoResponseDTO;
import com.ecommerce.service.PedidoService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Controller para gerenciamento de pedidos
 */
public class PedidoController {
    
    private final PedidoService pedidoService;
    
    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }
    
    public void criarPedido(Context ctx) {
        try {
            String userId = ctx.attribute("userId");
            if (userId == null) {
                ctx.status(HttpStatus.UNAUTHORIZED);
                ctx.json(new AuthController.ErrorResponse("Não autorizado", "Usuário não autenticado"));
                return;
            }
            
            PedidoRequestDTO request = ctx.bodyAsClass(PedidoRequestDTO.class);
            
            String idempotencyKey = ctx.header("Idempotency-Key");
            if (idempotencyKey == null || idempotencyKey.isEmpty()) {
                idempotencyKey = UUID.randomUUID().toString();
            }
            request.setIdempotencyKey(idempotencyKey);
            
            request.setClienteId(UUID.fromString(userId));
            
            PedidoResponseDTO response = pedidoService.criarPedido(request);
            
            ctx.status(HttpStatus.CREATED);
            ctx.header("Location", "/api/pedidos/" + response.getId());
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(response);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("Erro ao criar pedido", e.getMessage()));
        }
    }
    
    public void getMeusPedidos(Context ctx) {
        try {
            String userId = ctx.attribute("userId");
            if (userId == null) {
                ctx.status(HttpStatus.UNAUTHORIZED);
                ctx.json(new AuthController.ErrorResponse("Não autorizado", "Usuário não autenticado"));
                return;
            }
            
            var pedidos = pedidoService.getMeusPedidos(UUID.fromString(userId));
            
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.status(HttpStatus.OK);
            ctx.json(pedidos);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar seus pedidos", e.getMessage()));
        }
    }
    
    public void getPedidosAdmin(Context ctx) {
        try {
            String userRole = ctx.attribute("userRole");
            
            if (!"MANAGER".equals(userRole)) {
                ctx.status(HttpStatus.FORBIDDEN);
                ctx.json(new AuthController.ErrorResponse("Acesso negado", "Apenas administradores podem acessar"));
                return;
            }
            
            var pedidos = pedidoService.getPedidosAdmin();
            
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.status(HttpStatus.OK);
            ctx.json(pedidos);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar pedidos", e.getMessage()));
        }
    }
    
    /**
     * POST /api/pedidos/confirmar - Confirmar pedido com endereço e pagamento (autenticado)
     */
    public void confirmarPedido(Context ctx) {
        try {
            System.out.println("🚀 CONTROLLER PEDIDO - INICIO confirmarPedido()");
            
            // Extrair userId do JWT
            String userId = ctx.attribute("userId");
            if (userId == null) {
                System.err.println("❌ CONTROLLER PEDIDO - Usuário não autenticado");
                ctx.status(HttpStatus.UNAUTHORIZED);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Não autorizado", "Usuário não autenticado"));
                return;
            }
            System.out.println("🔑 CONTROLLER PEDIDO - UserId: " + userId);
            
            UUID clienteId = UUID.fromString(userId);
            ConfirmarPedidoRequestDTO request = ctx.bodyAsClass(ConfirmarPedidoRequestDTO.class);
            System.out.println("📦 CONTROLLER PEDIDO - Request recebido: " + request);
            System.out.println("🗺 CONTROLLER PEDIDO - EndereçoId: " + request.getEnderecoId());
            if (request.getPagamento() != null) {
                System.out.println("💳 CONTROLLER PEDIDO - Método Pagamento: " + request.getPagamento().getMetodo());
            }
            
            // Verificar idempotency key
            String idempotencyKey = ctx.header("Idempotency-Key");
            if (idempotencyKey == null || idempotencyKey.isEmpty()) {
                idempotencyKey = request.getIdempotencyKey();
                if (idempotencyKey == null || idempotencyKey.isEmpty()) {
                    idempotencyKey = UUID.randomUUID().toString();
                }
            }
            request.setIdempotencyKey(idempotencyKey);
            
            // Confirmar pedido com endereço e simulação de pagamento
            System.out.println("🎭 CONTROLLER PEDIDO - Chamando pedidoService.confirmarPedido()...");
            PedidoResponseDTO response = pedidoService.confirmarPedido(clienteId, request);
            System.out.println("✅ CONTROLLER PEDIDO - Pedido criado com sucesso: " + response.getNumero());
            
            ctx.status(HttpStatus.CREATED);
            ctx.header("Location", "/api/pedidos/" + response.getId());
            ctx.header("Cache-Control", "no-store");
            ctx.json(response);
            
            System.out.println("🎆 CONTROLLER PEDIDO - Response enviado com sucesso para o frontend");
            System.out.println("✔️ CONTROLLER PEDIDO - FIM confirmarPedido() - SUCESSO");
            
        } catch (IllegalArgumentException e) {
            System.err.println("❌ CONTROLLER PEDIDO - IllegalArgumentException: " + e.getMessage());
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.header("Cache-Control", "no-store");
            ctx.json(new AuthController.ErrorResponse("Dados inválidos", e.getMessage()));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Carrinho vazio")) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Carrinho vazio", e.getMessage()));
            } else if (e.getMessage().contains("não encontrad")) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Não encontrado", e.getMessage()));
            } else if (e.getMessage().contains("não pertence")) {
                ctx.status(HttpStatus.FORBIDDEN);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Acesso negado", e.getMessage()));
            } else if (e.getMessage().contains("Pagamento")) {
                ctx.status(HttpStatus.PAYMENT_REQUIRED);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Erro de pagamento", e.getMessage()));
            } else {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Erro ao confirmar pedido", e.getMessage()));
            }
        } catch (Exception e) {
            System.err.println("❌ CONTROLLER PEDIDO - Exception geral: " + e.getMessage());
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.header("Cache-Control", "no-store");
            ctx.json(new AuthController.ErrorResponse("Erro ao confirmar pedido", e.getMessage()));
        }
    }
    
    /**
     * POST /pedidos/{clienteId} - Confirmar pedido a partir do carrinho (compatibilidade)
     */
    public void confirmarPedidoLegacy(Context ctx) {
        try {
            UUID clienteId = UUID.fromString(ctx.pathParam("clienteId"));
            PedidoRequestDTO request = ctx.bodyAsClass(PedidoRequestDTO.class);
            
            String idempotencyKey = ctx.header("Idempotency-Key");
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                request.setIdempotencyKey(idempotencyKey);
            }
            
            PedidoResponseDTO response = pedidoService.confirmarPedido(clienteId, request);
            
            ctx.status(HttpStatus.CREATED);
            ctx.header("Location", "/pedidos/" + response.getId());
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.json(response);
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("ID de cliente inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("Erro ao confirmar pedido", e.getMessage()));
        }
    }
    
    /**
     * GET /pedidos - Listar pedidos (admin vê todos, usuários veem apenas os próprios)
     */
    public void findAll(Context ctx) {
        try {
            String userRole = ctx.attribute("userRole");
            String userId = ctx.attribute("userId");
            
            var pedidos = ("MANAGER".equals(userRole)) 
                ? pedidoService.findAll() 
                : pedidoService.findByCliente(UUID.fromString(userId));
            
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.status(HttpStatus.OK);
            ctx.json(pedidos);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar pedidos", e.getMessage()));
        }
    }
    
    /**
     * GET /pedidos/{id} - Buscar pedido por ID
     */
    public void findById(Context ctx) {
        try {
            UUID id = UUID.fromString(ctx.pathParam("id"));
            
            var pedidoOpt = pedidoService.findById(id);
            if (pedidoOpt.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(new AuthController.ErrorResponse("Pedido não encontrado", "ID: " + id));
                return;
            }
            
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.status(HttpStatus.OK);
            ctx.json(pedidoOpt.get());
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("ID inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar pedido", e.getMessage()));
        }
    }
    
    /**
     * GET /pedidos/cliente/{clienteId} - Listar pedidos do cliente
     */
    public void findByCliente(Context ctx) {
        try {
            UUID clienteId = UUID.fromString(ctx.pathParam("clienteId"));
            
            var pedidos = pedidoService.findByCliente(clienteId);
            
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.status(HttpStatus.OK);
            ctx.json(pedidos);
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("ID de cliente inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar pedidos", e.getMessage()));
        }
    }
    
    /**
     * GET /pedidos/status/{status} - Listar pedidos por status
     */
    public void findByStatus(Context ctx) {
        try {
            String statusStr = ctx.pathParam("status").toUpperCase();
            StatusPedido status = StatusPedido.valueOf(statusStr);
            
            var pedidos = pedidoService.findByStatus(status);
            
            ctx.status(HttpStatus.OK);
            ctx.json(pedidos);
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("Status inválido", 
                "Status válidos: NOVO, PROCESSANDO, PAGO, ENVIADO, ENTREGUE, CANCELADO"));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar pedidos", e.getMessage()));
        }
    }
    
    /**
     * GET /pedidos/periodo - Listar pedidos entre datas
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
            
            var pedidos = pedidoService.findByPeriodo(inicio, fim);
            
            ctx.status(HttpStatus.OK);
            ctx.json(pedidos);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("Erro nos parâmetros de data", e.getMessage()));
        }
    }
    
    /**
     * PUT /pedidos/{id}/status/{status} - Atualizar status do pedido
     */
    public void atualizarStatus(Context ctx) {
        try {
            UUID pedidoId = UUID.fromString(ctx.pathParam("id"));
            String statusStr = ctx.pathParam("status").toUpperCase();
            StatusPedido novoStatus = StatusPedido.valueOf(statusStr);
            
            PedidoResponseDTO response = pedidoService.atualizarStatus(pedidoId, novoStatus);
            
            ctx.status(HttpStatus.OK);
            ctx.json(response);
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("Parâmetros inválidos", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("Erro ao atualizar status", e.getMessage()));
        }
    }
    
    /**
     * PUT /pedidos/{id}/cancelar/{clienteId} - Cancelar pedido
     */
    public void cancelarPedido(Context ctx) {
        try {
            UUID pedidoId = UUID.fromString(ctx.pathParam("id"));
            UUID clienteId = UUID.fromString(ctx.pathParam("clienteId"));
            
            PedidoResponseDTO response = pedidoService.cancelarPedido(clienteId, pedidoId);
            
            ctx.status(HttpStatus.OK);
            ctx.json(response);
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("IDs inválidos", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("Erro ao cancelar pedido", e.getMessage()));
        }
    }
    
    /**
     * GET /pedidos/count/status/{status} - Contar pedidos por status
     */
    public void countByStatus(Context ctx) {
        try {
            String statusStr = ctx.pathParam("status").toUpperCase();
            StatusPedido status = StatusPedido.valueOf(statusStr);
            
            long count = pedidoService.countByStatus(status);
            
            ctx.status(HttpStatus.OK);
            ctx.json(new CategoriaController.CountResponse(count));
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("Status inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao contar pedidos", e.getMessage()));
        }
    }
    
    /**
     * GET /pedidos/count/cliente/{clienteId} - Contar pedidos por cliente
     */
    public void countByCliente(Context ctx) {
        try {
            UUID clienteId = UUID.fromString(ctx.pathParam("clienteId"));
            
            long count = pedidoService.countByCliente(clienteId);
            
            ctx.status(HttpStatus.OK);
            ctx.json(new CategoriaController.CountResponse(count));
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("ID de cliente inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao contar pedidos", e.getMessage()));
        }
    }
    
    /**
     * POST /api/pedidos/checkout - Finalizar carrinho e criar pedido
     */
    public void checkout(Context ctx) {
        try {
            String userId = ctx.attribute("userId");
            if (userId == null) {
                ctx.status(HttpStatus.UNAUTHORIZED);
                ctx.json(new AuthController.ErrorResponse("Não autorizado", "Usuário não autenticado"));
                return;
            }
            
            // Pegar idempotency key do header ou body
            String idempotencyKey = ctx.header("Idempotency-Key");
            if (idempotencyKey == null || idempotencyKey.isEmpty()) {
                // Tentar pegar do body
                var body = ctx.bodyAsClass(java.util.Map.class);
                idempotencyKey = (String) body.get("idempotencyKey");
            }
            
            // Verificar se já existe pedido com essa idempotency key
            boolean pedidoExistente = false;
            if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
                pedidoExistente = pedidoService.existePedidoComIdempotencyKey(idempotencyKey);
            }
            
            // Chamar finalizarCarrinho do serviço
            PedidoResponseDTO response = pedidoService.finalizarCarrinho(
                UUID.fromString(userId), 
                idempotencyKey
            );
            
            // Retornar 200 se pedido já existia (idempotência), 201 se novo
            if (pedidoExistente) {
                ctx.status(HttpStatus.OK); // 200 para pedido existente
            } else {
                ctx.status(HttpStatus.CREATED); // 201 para pedido novo
            }
            
            ctx.header("Location", "/api/pedidos/" + response.getId());
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(response);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("Erro ao finalizar carrinho", e.getMessage()));
        }
    }
}