package com.ecommerce.controller;

import com.ecommerce.dto.request.ClienteRequestDTO;
import com.ecommerce.dto.response.ClienteResponseDTO;
import com.ecommerce.service.ClienteService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.UUID;

/**
 * Controller para gerenciamento de clientes
 */
public class ClienteController {
    
    private final ClienteService clienteService;
    
    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }
    
    /**
     * POST /clientes - Criar novo cliente
     */
    public void create(Context ctx) {
        try {
            ClienteRequestDTO request = ctx.bodyAsClass(ClienteRequestDTO.class);
            
            ClienteResponseDTO response = clienteService.create(request);
            
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
            ctx.json(new AuthController.ErrorResponse("Erro ao criar cliente", e.getMessage()));
        }
    }
    
    /**
     * GET /clientes - Listar todos os clientes
     */
    public void findAll(Context ctx) {
        try {
            var clientes = clienteService.findAll();
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(clientes);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar clientes", e.getMessage()));
        }
    }
    
    /**
     * GET /clientes/{id} - Buscar cliente por ID
     */
    public void findById(Context ctx) {
        try {
            UUID id = UUID.fromString(ctx.pathParam("id"));
            
            var clienteOpt = clienteService.findById(id);
            if (clienteOpt.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
                ctx.header("Pragma", "no-cache");
                ctx.header("Expires", "0");
                ctx.json(new AuthController.ErrorResponse("Cliente não encontrado", "ID: " + id));
                return;
            }
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(clienteOpt.get());
            
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
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar cliente", e.getMessage()));
        }
    }
    
    /**
     * GET /clientes/{id}/enderecos - Buscar cliente com endereços
     */
    public void findByIdWithEnderecos(Context ctx) {
        try {
            UUID id = UUID.fromString(ctx.pathParam("id"));
            
            var clienteOpt = clienteService.findByIdWithEnderecos(id);
            if (clienteOpt.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
                ctx.header("Pragma", "no-cache");
                ctx.header("Expires", "0");
                ctx.json(new AuthController.ErrorResponse("Cliente não encontrado", "ID: " + id));
                return;
            }
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(clienteOpt.get());
            
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
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar cliente", e.getMessage()));
        }
    }
    
    /**
     * GET /clientes/email/{email} - Buscar cliente por email
     */
    public void findByEmail(Context ctx) {
        try {
            String email = ctx.pathParam("email");
            
            var clienteOpt = clienteService.findByEmail(email);
            if (clienteOpt.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
                ctx.header("Pragma", "no-cache");
                ctx.header("Expires", "0");
                ctx.json(new AuthController.ErrorResponse("Cliente não encontrado", "Email: " + email));
                return;
            }
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(clienteOpt.get());
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar cliente", e.getMessage()));
        }
    }
    
    /**
     * GET /clientes/buscar/{nome} - Buscar clientes por nome
     */
    public void findByNome(Context ctx) {
        try {
            String nome = ctx.pathParam("nome");
            
            var clientes = clienteService.findByNome(nome);
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(clientes);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar clientes", e.getMessage()));
        }
    }
    
    /**
     * PUT /clientes/{id} - Atualizar cliente
     */
    public void update(Context ctx) {
        try {
            UUID id = UUID.fromString(ctx.pathParam("id"));
            ClienteRequestDTO request = ctx.bodyAsClass(ClienteRequestDTO.class);
            
            ClienteResponseDTO response = clienteService.update(id, request);
            
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
            ctx.json(new AuthController.ErrorResponse("Erro ao atualizar cliente", e.getMessage()));
        }
    }
    
    /**
     * DELETE /clientes/{id} - Remover cliente
     */
    public void delete(Context ctx) {
        try {
            UUID id = UUID.fromString(ctx.pathParam("id"));
            
            clienteService.delete(id);
            
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
            ctx.json(new AuthController.ErrorResponse("Erro ao remover cliente", e.getMessage()));
        }
    }
    
    /**
     * GET /clientes/count - Contar total de clientes
     */
    public void count(Context ctx) {
        try {
            long count = clienteService.count();
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new CategoriaController.CountResponse(count));
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new AuthController.ErrorResponse("Erro ao contar clientes", e.getMessage()));
        }
    }
}