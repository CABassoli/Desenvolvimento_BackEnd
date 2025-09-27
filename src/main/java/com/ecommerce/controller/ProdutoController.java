package com.ecommerce.controller;

import com.ecommerce.dto.request.ProdutoRequestDTO;
import com.ecommerce.dto.response.ProdutoResponseDTO;
import com.ecommerce.service.ProdutoService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Controller para gerenciamento de produtos
 */
public class ProdutoController {
    
    private final ProdutoService produtoService;
    
    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }
    
    /**
     * POST /produtos - Criar novo produto
     */
    public void create(Context ctx) {
        try {
            ProdutoRequestDTO request = ctx.bodyAsClass(ProdutoRequestDTO.class);
            
            ProdutoResponseDTO response = produtoService.create(request);
            
            ctx.status(HttpStatus.CREATED);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(response);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("Erro ao criar produto", e.getMessage()));
        }
    }
    
    /**
     * GET /produtos - Listar todos os produtos
     */
    public void findAll(Context ctx) {
        try {
            var produtos = produtoService.findAll();
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(produtos);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar produtos", e.getMessage()));
        }
    }
    
    /**
     * GET /produtos/{id} - Buscar produto por ID
     */
    public void findById(Context ctx) {
        try {
            UUID id = UUID.fromString(ctx.pathParam("id"));
            
            var produtoOpt = produtoService.findById(id);
            if (produtoOpt.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
                ctx.header("Pragma", "no-cache");
                ctx.header("Expires", "0");
                ctx.json(new AuthController.ErrorResponse("Produto não encontrado", "ID: " + id));
                return;
            }
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(produtoOpt.get());
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("ID inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar produto", e.getMessage()));
        }
    }
    
    /**
     * GET /produtos/codigo/{codigo} - Buscar produto por código de barras
     */
    public void findByCodigoBarras(Context ctx) {
        try {
            String codigoBarras = ctx.pathParam("codigo");
            
            var produtoOpt = produtoService.findByCodigoBarras(codigoBarras);
            if (produtoOpt.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
                ctx.header("Pragma", "no-cache");
                ctx.header("Expires", "0");
                ctx.json(new AuthController.ErrorResponse("Produto não encontrado", "Código: " + codigoBarras));
                return;
            }
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(produtoOpt.get());
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar produto", e.getMessage()));
        }
    }
    
    /**
     * GET /produtos/categoria/{categoriaId} - Listar produtos por categoria
     */
    public void findByCategoria(Context ctx) {
        try {
            UUID categoriaId = UUID.fromString(ctx.pathParam("categoriaId"));
            
            var produtos = produtoService.findByCategoria(categoriaId);
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(produtos);
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("ID de categoria inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar produtos", e.getMessage()));
        }
    }
    
    /**
     * GET /produtos/buscar/{nome} - Buscar produtos por nome
     */
    public void findByNome(Context ctx) {
        try {
            String nome = ctx.pathParam("nome");
            
            var produtos = produtoService.findByNome(nome);
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(produtos);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar produtos", e.getMessage()));
        }
    }
    
    /**
     * GET /produtos/preco - Buscar produtos por faixa de preço
     * Query params: min, max
     */
    public void findByPrecoRange(Context ctx) {
        try {
            String minStr = ctx.queryParam("min");
            String maxStr = ctx.queryParam("max");
            
            if (minStr == null || maxStr == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new AuthController.ErrorResponse("Parâmetros obrigatórios", "min e max são obrigatórios"));
                return;
            }
            
            BigDecimal precoMin = new BigDecimal(minStr);
            BigDecimal precoMax = new BigDecimal(maxStr);
            
            var produtos = produtoService.findByPrecoRange(precoMin, precoMax);
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(produtos);
            
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("Preços inválidos", "min e max devem ser números válidos"));
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar produtos", e.getMessage()));
        }
    }
    
    /**
     * PUT /produtos/{id} - Atualizar produto
     */
    public void update(Context ctx) {
        try {
            UUID id = UUID.fromString(ctx.pathParam("id"));
            ProdutoRequestDTO request = ctx.bodyAsClass(ProdutoRequestDTO.class);
            
            ProdutoResponseDTO response = produtoService.update(id, request);
            
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
            ctx.json(new AuthController.ErrorResponse("ID inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("Erro ao atualizar produto", e.getMessage()));
        }
    }
    
    /**
     * DELETE /produtos/{id} - Remover produto
     */
    public void delete(Context ctx) {
        try {
            UUID id = UUID.fromString(ctx.pathParam("id"));
            
            produtoService.delete(id);
            
            ctx.status(HttpStatus.NO_CONTENT);
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("ID inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("Erro ao remover produto", e.getMessage()));
        }
    }
}