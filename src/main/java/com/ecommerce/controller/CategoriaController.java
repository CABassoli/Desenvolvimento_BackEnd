package com.ecommerce.controller;

import com.ecommerce.dto.request.CategoriaRequestDTO;
import com.ecommerce.dto.response.CategoriaResponseDTO;
import com.ecommerce.service.CategoriaService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.UUID;

/**
 * Controller para gerenciamento de categorias
 */
public class CategoriaController {
    
    private final CategoriaService categoriaService;
    
    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }
    
    /**
     * POST /categorias - Criar nova categoria
     */
    public void create(Context ctx) {
        try {
            CategoriaRequestDTO request = ctx.bodyAsClass(CategoriaRequestDTO.class);
            
            CategoriaResponseDTO response = categoriaService.create(request);
            
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
            ctx.json(new AuthController.ErrorResponse("Erro ao criar categoria", e.getMessage()));
        }
    }
    
    /**
     * GET /categorias - Listar todas as categorias
     */
    public void findAll(Context ctx) {
        try {
            var categorias = categoriaService.findAll();
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(categorias);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar categorias", e.getMessage()));
        }
    }
    
    /**
     * GET /categorias/{id} - Buscar categoria por ID
     */
    public void findById(Context ctx) {
        try {
            UUID id = UUID.fromString(ctx.pathParam("id"));
            
            var categoriaOpt = categoriaService.findById(id);
            if (categoriaOpt.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
                ctx.header("Pragma", "no-cache");
                ctx.header("Expires", "0");
                ctx.json(new AuthController.ErrorResponse("Categoria não encontrada", "ID: " + id));
                return;
            }
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(categoriaOpt.get());
            
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
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar categoria", e.getMessage()));
        }
    }
    
    /**
     * GET /categorias/nome/{nome} - Buscar categoria por nome
     */
    public void findByNome(Context ctx) {
        try {
            String nome = ctx.pathParam("nome");
            
            var categoriaOpt = categoriaService.findByNome(nome);
            if (categoriaOpt.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
                ctx.header("Pragma", "no-cache");
                ctx.header("Expires", "0");
                ctx.json(new AuthController.ErrorResponse("Categoria não encontrada", "Nome: " + nome));
                return;
            }
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(categoriaOpt.get());
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar categoria", e.getMessage()));
        }
    }
    
    /**
     * PUT /categorias/{id} - Atualizar categoria
     */
    public void update(Context ctx) {
        try {
            UUID id = UUID.fromString(ctx.pathParam("id"));
            CategoriaRequestDTO request = ctx.bodyAsClass(CategoriaRequestDTO.class);
            
            CategoriaResponseDTO response = categoriaService.update(id, request);
            
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
            ctx.json(new AuthController.ErrorResponse("Erro ao atualizar categoria", e.getMessage()));
        }
    }
    
    /**
     * DELETE /categorias/{id} - Remover categoria
     */
    public void delete(Context ctx) {
        try {
            UUID id = UUID.fromString(ctx.pathParam("id"));
            
            categoriaService.delete(id);
            
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
            ctx.json(new AuthController.ErrorResponse("Erro ao remover categoria", e.getMessage()));
        }
    }
    
    /**
     * GET /categorias/count - Contar total de categorias
     */
    public void count(Context ctx) {
        try {
            long count = categoriaService.count();
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new CountResponse(count));
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("Erro ao contar categorias", e.getMessage()));
        }
    }
    
    // Classe auxiliar para resposta de contagem
    public static class CountResponse {
        private long count;
        
        public CountResponse(long count) {
            this.count = count;
        }
        
        public long getCount() { return count; }
    }
}