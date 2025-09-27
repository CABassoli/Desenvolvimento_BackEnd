package com.ecommerce.controller;

import com.ecommerce.dto.SimulacaoPagamentoRequestDTO;
import com.ecommerce.dto.SimulacaoPagamentoResponseDTO;
import com.ecommerce.dto.DadosCartaoDTO;
import com.ecommerce.dto.request.PagamentoRequestDTO;
import com.ecommerce.dto.response.PagamentoResponseDTO;
import com.ecommerce.service.PagamentoService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Controller para processamento de pagamentos
 */
public class PagamentoController {
    
    private final PagamentoService pagamentoService;
    
    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }
    
    /**
     * POST /api/pagamentos/simular - Simular processamento de pagamento
     */
    public void simularPagamento(Context ctx) {
        try {
            // Extrair userId do JWT
            String userId = ctx.attribute("userId");
            if (userId == null) {
                ctx.status(HttpStatus.UNAUTHORIZED);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Não autorizado", "Usuário não autenticado"));
                return;
            }
            
            SimulacaoPagamentoRequestDTO request = ctx.bodyAsClass(SimulacaoPagamentoRequestDTO.class);
            
            // Validações básicas
            if (request.getMetodo() == null || request.getValor() == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Dados inválidos", "Método e valor são obrigatórios"));
                return;
            }
            
            // Se método é CARTAO, validar dados do cartão
            if ("CARTAO".equals(request.getMetodo()) && request.getDadosCartao() == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Dados inválidos", "Dados do cartão são obrigatórios para método CARTAO"));
                return;
            }
            
            // Simular processamento do pagamento
            SimulacaoPagamentoResponseDTO response = pagamentoService.simularPagamento(request, UUID.fromString(userId));
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store");
            ctx.json(response);
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.header("Cache-Control", "no-store");
            ctx.json(new AuthController.ErrorResponse("Dados inválidos", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.header("Cache-Control", "no-store");
            ctx.json(new AuthController.ErrorResponse("Erro ao simular pagamento", e.getMessage()));
        }
    }
    
    /**
     * POST /pagamentos/{pedidoId}/pix - Processar pagamento PIX
     */
    public void processarPagamentoPix(Context ctx) {
        try {
            UUID pedidoId = UUID.fromString(ctx.pathParam("pedidoId"));
            PagamentoRequestDTO request = ctx.bodyAsClass(PagamentoRequestDTO.class);
            
            PagamentoResponseDTO response = pagamentoService.processarPagamentoPix(pedidoId, request);
            
            ctx.status(HttpStatus.CREATED);
            ctx.json(response);
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("ID de pedido inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("Erro no pagamento PIX", e.getMessage()));
        }
    }
    
    
    /**
     * POST /pagamentos/{pedidoId}/boleto - Processar pagamento com boleto
     */
    public void processarPagamentoBoleto(Context ctx) {
        try {
            UUID pedidoId = UUID.fromString(ctx.pathParam("pedidoId"));
            PagamentoRequestDTO request = ctx.bodyAsClass(PagamentoRequestDTO.class);
            
            PagamentoResponseDTO response = pagamentoService.processarPagamentoBoleto(pedidoId, request);
            
            ctx.status(HttpStatus.CREATED);
            ctx.json(response);
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("ID de pedido inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("Erro no pagamento com boleto", e.getMessage()));
        }
    }
    
    /**
     * PUT /pagamentos/boleto/confirmar/{linhaDigitavel} - Confirmar pagamento de boleto
     */
    public void confirmarPagamentoBoleto(Context ctx) {
        try {
            String linhaDigitavel = ctx.pathParam("linhaDigitavel");
            
            PagamentoResponseDTO response = pagamentoService.confirmarPagamentoBoleto(linhaDigitavel);
            
            ctx.status(HttpStatus.OK);
            ctx.json(response);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("Erro na confirmação do boleto", e.getMessage()));
        }
    }
    
    /**
     * GET /pagamentos - Listar todos os pagamentos
     */
    public void findAll(Context ctx) {
        try {
            var pagamentos = pagamentoService.findAll();
            
            ctx.status(HttpStatus.OK);
            ctx.json(pagamentos);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar pagamentos", e.getMessage()));
        }
    }
    
    /**
     * GET /pagamentos/{id} - Buscar pagamento por ID
     */
    public void findById(Context ctx) {
        try {
            UUID id = UUID.fromString(ctx.pathParam("id"));
            
            var pagamentoOpt = pagamentoService.findById(id);
            if (pagamentoOpt.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(new AuthController.ErrorResponse("Pagamento não encontrado", "ID: " + id));
                return;
            }
            
            ctx.status(HttpStatus.OK);
            ctx.json(pagamentoOpt.get());
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("ID inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar pagamento", e.getMessage()));
        }
    }
    
    /**
     * GET /pagamentos/pedido/{pedidoId} - Buscar pagamento por pedido
     */
    public void findByPedido(Context ctx) {
        try {
            UUID pedidoId = UUID.fromString(ctx.pathParam("pedidoId"));
            
            var pagamentoOpt = pagamentoService.findByPedido(pedidoId);
            if (pagamentoOpt.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(new AuthController.ErrorResponse("Pagamento não encontrado", "Pedido ID: " + pedidoId));
                return;
            }
            
            ctx.status(HttpStatus.OK);
            ctx.json(pagamentoOpt.get());
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("ID de pedido inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar pagamento", e.getMessage()));
        }
    }
    
    /**
     * GET /pagamentos/pix - Listar pagamentos PIX
     */
    public void findAllPix(Context ctx) {
        try {
            var pagamentos = pagamentoService.findAllPix();
            
            ctx.status(HttpStatus.OK);
            ctx.json(pagamentos);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar pagamentos PIX", e.getMessage()));
        }
    }
    
    /**
     * GET /pagamentos/cartao - Listar pagamentos com cartão
     */
    public void findAllCartao(Context ctx) {
        try {
            var pagamentos = pagamentoService.findAllCartao();
            
            ctx.status(HttpStatus.OK);
            ctx.json(pagamentos);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar pagamentos com cartão", e.getMessage()));
        }
    }
    
    /**
     * GET /pagamentos/boleto - Listar pagamentos com boleto
     */
    public void findAllBoleto(Context ctx) {
        try {
            var pagamentos = pagamentoService.findAllBoleto();
            
            ctx.status(HttpStatus.OK);
            ctx.json(pagamentos);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar pagamentos com boleto", e.getMessage()));
        }
    }
    
    /**
     * GET /pagamentos/total - Calcular total de pagamentos
     */
    public void calcularTotal(Context ctx) {
        try {
            BigDecimal total = pagamentoService.calcularTotalPagamentos();
            
            ctx.status(HttpStatus.OK);
            ctx.json(new CarrinhoController.TotalResponse(total));
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao calcular total", e.getMessage()));
        }
    }
    
    /**
     * GET /pagamentos/count - Contar total de pagamentos
     */
    public void count(Context ctx) {
        try {
            long count = pagamentoService.count();
            
            ctx.status(HttpStatus.OK);
            ctx.json(new CategoriaController.CountResponse(count));
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao contar pagamentos", e.getMessage()));
        }
    }
}