package com.ecommerce.controller;

import com.ecommerce.dto.request.ItemCarrinhoRequestDTO;
import com.ecommerce.dto.response.CarrinhoResponseDTO;
import com.ecommerce.dto.response.ItemCarrinhoResponseDTO;
import com.ecommerce.service.CarrinhoService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Controller para gerenciamento do carrinho de compras
 */
public class CarrinhoController {
    
    private final CarrinhoService carrinhoService;
    
    public CarrinhoController(CarrinhoService carrinhoService) {
        this.carrinhoService = carrinhoService;
    }
    
    /**
     * GET /api/carrinho - Buscar ou criar carrinho do usuário autenticado
     */
    public void getCarrinho(Context ctx) {
        try {
            // Extrair userId do JWT (adicionado pelo JwtMiddleware)
            String userId = ctx.attribute("userId");
            if (userId == null) {
                ctx.status(HttpStatus.UNAUTHORIZED);
                setAntiCacheHeaders(ctx);
                ctx.json(new AuthController.ErrorResponse("Não autenticado", "Token JWT inválido ou ausente"));
                return;
            }
            
            UUID userUuid = UUID.fromString(userId);
            
            // Busca ou cria carrinho - nunca retorna null
            CarrinhoResponseDTO carrinho = carrinhoService.findOrCreateByUserId(userUuid);
            
            ctx.status(HttpStatus.OK);
            setAntiCacheHeaders(ctx);
            ctx.json(carrinho);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            setAntiCacheHeaders(ctx);
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar carrinho", e.getMessage()));
        }
    }
    
    /**
     * POST /api/carrinho/item - Adicionar item ao carrinho
     */
    public void addItem(Context ctx) {
        try {
            // Extrair userId do JWT
            String userId = ctx.attribute("userId");
            if (userId == null) {
                ctx.status(HttpStatus.UNAUTHORIZED);
                setAntiCacheHeaders(ctx);
                ctx.json(new AuthController.ErrorResponse("Não autenticado", "Token JWT inválido ou ausente"));
                return;
            }
            
            UUID userUuid = UUID.fromString(userId);
            ItemCarrinhoRequestDTO request = ctx.bodyAsClass(ItemCarrinhoRequestDTO.class);
            
            // Validar entrada
            if (request.getProdutoId() == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                setAntiCacheHeaders(ctx);
                ctx.json(new AuthController.ErrorResponse("Dados inválidos", "ID do produto é obrigatório"));
                return;
            }
            
            // Normalizar quantidade (1-999)
            Integer quantidade = request.getQuantidade();
            if (quantidade == null || quantidade < 1) {
                quantidade = 1;
            } else if (quantidade > 999) {
                quantidade = 999;
            }
            
            // Adicionar item e retornar carrinho atualizado
            CarrinhoResponseDTO carrinho = carrinhoService.addItem(userUuid, request.getProdutoId(), quantidade);
            
            ctx.status(HttpStatus.OK);
            setAntiCacheHeaders(ctx);
            ctx.json(carrinho);
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            setAntiCacheHeaders(ctx);
            ctx.json(new AuthController.ErrorResponse("Dados inválidos", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            setAntiCacheHeaders(ctx);
            ctx.json(new AuthController.ErrorResponse("Erro ao adicionar item", e.getMessage()));
        }
    }
    
    /**
     * DELETE /api/carrinho/item/:produtoId - Remover item do carrinho
     */
    public void removeItem(Context ctx) {
        try {
            // Extrair userId do JWT
            String userId = ctx.attribute("userId");
            if (userId == null) {
                ctx.status(HttpStatus.UNAUTHORIZED);
                setAntiCacheHeaders(ctx);
                ctx.json(new AuthController.ErrorResponse("Não autenticado", "Token JWT inválido ou ausente"));
                return;
            }
            
            // Extrair produtoId do path
            String produtoIdStr = ctx.pathParam("produtoId");
            UUID userUuid = UUID.fromString(userId);
            UUID produtoId = UUID.fromString(produtoIdStr);
            
            // Remover item e retornar carrinho atualizado
            carrinhoService.removeItem(userUuid, produtoId);
            CarrinhoResponseDTO carrinho = carrinhoService.findOrCreateByUserId(userUuid);
            
            ctx.status(HttpStatus.OK);
            setAntiCacheHeaders(ctx);
            ctx.json(carrinho);
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            setAntiCacheHeaders(ctx);
            ctx.json(new AuthController.ErrorResponse("ID inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            setAntiCacheHeaders(ctx);
            ctx.json(new AuthController.ErrorResponse("Erro ao remover item", e.getMessage()));
        }
    }
    
    /**
     * DELETE /api/carrinho - Limpar carrinho completamente
     */
    public void clearCarrinho(Context ctx) {
        try {
            // Extrair userId do JWT
            String userId = ctx.attribute("userId");
            if (userId == null) {
                ctx.status(HttpStatus.UNAUTHORIZED);
                setAntiCacheHeaders(ctx);
                ctx.json(new AuthController.ErrorResponse("Não autenticado", "Token JWT inválido ou ausente"));
                return;
            }
            
            UUID userUuid = UUID.fromString(userId);
            
            // Limpar carrinho
            carrinhoService.clearCarrinho(userUuid);
            
            // Retornar 204 No Content para DELETE bem-sucedido
            ctx.status(HttpStatus.NO_CONTENT);
            setAntiCacheHeaders(ctx);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            setAntiCacheHeaders(ctx);
            ctx.json(new AuthController.ErrorResponse("Erro ao limpar carrinho", e.getMessage()));
        }
    }
    
    /**
     * Adiciona headers anti-cache em todas as respostas
     */
    private void setAntiCacheHeaders(Context ctx) {
        ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
        ctx.header("Pragma", "no-cache");
        ctx.header("Expires", "0");
    }
    
    // Métodos legados mantidos para compatibilidade
    
    /**
     * PUT /carrinho/{clienteId}/itens/{itemId} - Atualizar quantidade do item (LEGADO)
     */
    public void updateItem(Context ctx) {
        try {
            UUID clienteId = UUID.fromString(ctx.pathParam("clienteId"));
            UUID itemId = UUID.fromString(ctx.pathParam("itemId"));
            ItemCarrinhoRequestDTO request = ctx.bodyAsClass(ItemCarrinhoRequestDTO.class);
            
            ItemCarrinhoResponseDTO response = carrinhoService.updateItem(clienteId, itemId, request);
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(response);
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("IDs inválidos", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("Erro ao atualizar item", e.getMessage()));
        }
    }
    
    
    
    /**
     * GET /carrinho/{clienteId}/itens - Listar itens do carrinho
     */
    public void getItens(Context ctx) {
        try {
            UUID clienteId = UUID.fromString(ctx.pathParam("clienteId"));
            
            var itens = carrinhoService.getItens(clienteId);
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(itens);
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("ID de cliente inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar itens", e.getMessage()));
        }
    }
    
    /**
     * GET /carrinho/{clienteId}/total - Calcular valor total do carrinho
     */
    public void calcularTotal(Context ctx) {
        try {
            UUID clienteId = UUID.fromString(ctx.pathParam("clienteId"));
            
            BigDecimal total = carrinhoService.calcularValorTotal(clienteId);
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new TotalResponse(total));
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("ID de cliente inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("Erro ao calcular total", e.getMessage()));
        }
    }
    
    /**
     * GET /carrinho/{clienteId}/count - Contar itens do carrinho
     */
    public void contarItens(Context ctx) {
        try {
            UUID clienteId = UUID.fromString(ctx.pathParam("clienteId"));
            
            int count = carrinhoService.contarItens(clienteId);
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new ItemCountResponse(count));
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("ID de cliente inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("Erro ao contar itens", e.getMessage()));
        }
    }
    
    // Classes auxiliares para respostas
    
    public static class TotalResponse {
        private BigDecimal total;
        
        public TotalResponse(BigDecimal total) {
            this.total = total;
        }
        
        public BigDecimal getTotal() { return total; }
    }
    
    public static class ItemCountResponse {
        private int count;
        
        public ItemCountResponse(int count) {
            this.count = count;
        }
        
        public int getCount() { return count; }
    }
}