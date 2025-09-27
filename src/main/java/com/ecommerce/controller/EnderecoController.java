package com.ecommerce.controller;

import com.ecommerce.dto.request.EnderecoRequestDTO;
import com.ecommerce.dto.response.EnderecoResponseDTO;
import com.ecommerce.service.EnderecoService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller para gerenciamento de endereços com autenticação JWT
 */
public class EnderecoController {
    
    private final EnderecoService enderecoService;
    
    public EnderecoController(EnderecoService enderecoService) {
        this.enderecoService = enderecoService;
    }
    
    /**
     * GET /api/enderecos - Listar endereços do cliente autenticado
     */
    public void listarEnderecos(Context ctx) {
        try {
            // Extrair userId do JWT (adicionado pelo JwtMiddleware)
            String userId = ctx.attribute("userId");
            if (userId == null) {
                ctx.status(HttpStatus.UNAUTHORIZED);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Não autorizado", "Usuário não autenticado"));
                return;
            }
            
            UUID clienteId = UUID.fromString(userId);
            List<EnderecoResponseDTO> enderecos = enderecoService.listarEnderecos(clienteId);
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store");
            ctx.json(enderecos);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.header("Cache-Control", "no-store");
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar endereços", e.getMessage()));
        }
    }
    
    /**
     * POST /api/enderecos - Criar novo endereço para cliente autenticado
     */
    public void criarEndereco(Context ctx) {
        try {
            // Extrair userId do JWT
            String userId = ctx.attribute("userId");
            if (userId == null) {
                ctx.status(HttpStatus.UNAUTHORIZED);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Não autorizado", "Usuário não autenticado"));
                return;
            }
            
            UUID clienteId = UUID.fromString(userId);
            EnderecoRequestDTO request = ctx.bodyAsClass(EnderecoRequestDTO.class);
            
            // Sobrescrever clienteId com o do usuário autenticado
            request.setClienteId(clienteId);
            
            EnderecoResponseDTO response = enderecoService.criarEndereco(clienteId, request);
            
            ctx.status(HttpStatus.CREATED);
            ctx.header("Cache-Control", "no-store");
            ctx.json(response);
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.header("Cache-Control", "no-store");
            ctx.json(new AuthController.ErrorResponse("Dados inválidos", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.header("Cache-Control", "no-store");
            ctx.json(new AuthController.ErrorResponse("Erro ao criar endereço", e.getMessage()));
        }
    }
    
    /**
     * PATCH /api/enderecos/{id} - Atualizar endereço ou definir como padrão
     */
    public void atualizarEndereco(Context ctx) {
        try {
            // Extrair userId do JWT
            String userId = ctx.attribute("userId");
            if (userId == null) {
                ctx.status(HttpStatus.UNAUTHORIZED);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Não autorizado", "Usuário não autenticado"));
                return;
            }
            
            UUID clienteId = UUID.fromString(userId);
            UUID enderecoId = UUID.fromString(ctx.pathParam("id"));
            
            // Parse do body do request
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            
            EnderecoResponseDTO response = null;
            
            // Verificar se é para definir como padrão
            if (body.containsKey("ehPadrao") && Boolean.TRUE.equals(body.get("ehPadrao"))) {
                response = enderecoService.definirEnderecoPadrao(clienteId, enderecoId);
            } else {
                // Atualizar campos do endereço
                EnderecoRequestDTO request = new EnderecoRequestDTO();
                
                if (body.containsKey("rua")) {
                    request.setRua((String) body.get("rua"));
                }
                if (body.containsKey("numero")) {
                    request.setNumero((String) body.get("numero"));
                }
                if (body.containsKey("cep")) {
                    request.setCep((String) body.get("cep"));
                }
                if (body.containsKey("cidade")) {
                    request.setCidade((String) body.get("cidade"));
                }
                if (body.containsKey("bairro")) {
                    request.setBairro((String) body.get("bairro"));
                }
                if (body.containsKey("estado")) {
                    request.setEstado((String) body.get("estado"));
                }
                if (body.containsKey("complemento")) {
                    request.setComplemento((String) body.get("complemento"));
                }
                
                request.setClienteId(clienteId);
                
                response = enderecoService.atualizarEndereco(clienteId, enderecoId, request);
            }
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store");
            ctx.json(response);
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.header("Cache-Control", "no-store");
            ctx.json(new AuthController.ErrorResponse("Dados inválidos", e.getMessage()));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("não encontrado")) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Não encontrado", e.getMessage()));
            } else if (e.getMessage().contains("não pertence")) {
                ctx.status(HttpStatus.FORBIDDEN);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Acesso negado", e.getMessage()));
            } else {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Erro ao atualizar endereço", e.getMessage()));
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.header("Cache-Control", "no-store");
            ctx.json(new AuthController.ErrorResponse("Erro ao atualizar endereço", e.getMessage()));
        }
    }
    
    /**
     * GET /api/enderecos/{id} - Buscar endereço específico do cliente autenticado
     */
    public void buscarEnderecoPorId(Context ctx) {
        try {
            // Extrair userId do JWT
            String userId = ctx.attribute("userId");
            if (userId == null) {
                ctx.status(HttpStatus.UNAUTHORIZED);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Não autorizado", "Usuário não autenticado"));
                return;
            }
            
            UUID clienteId = UUID.fromString(userId);
            UUID enderecoId = UUID.fromString(ctx.pathParam("id"));
            
            // Buscar endereço validando propriedade
            EnderecoResponseDTO response = enderecoService.buscarEnderecoPorIdECliente(clienteId, enderecoId);
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store");
            ctx.json(response);
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.header("Cache-Control", "no-store");
            ctx.json(new AuthController.ErrorResponse("ID inválido", e.getMessage()));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("não encontrado") || e.getMessage().contains("não pertence")) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Não encontrado", "Endereço não encontrado para este cliente"));
            } else {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Erro ao buscar endereço", e.getMessage()));
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.header("Cache-Control", "no-store");
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar endereço", e.getMessage()));
        }
    }
    
    /**
     * DELETE /api/enderecos/{id} - Remover endereço do cliente autenticado
     */
    public void removerEndereco(Context ctx) {
        try {
            // Extrair userId do JWT
            String userId = ctx.attribute("userId");
            if (userId == null) {
                ctx.status(HttpStatus.UNAUTHORIZED);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Não autorizado", "Usuário não autenticado"));
                return;
            }
            
            UUID clienteId = UUID.fromString(userId);
            UUID enderecoId = UUID.fromString(ctx.pathParam("id"));
            
            enderecoService.removerEndereco(clienteId, enderecoId);
            
            ctx.status(HttpStatus.NO_CONTENT);
            ctx.header("Cache-Control", "no-store");
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.header("Cache-Control", "no-store");
            ctx.json(new AuthController.ErrorResponse("ID inválido", e.getMessage()));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("não encontrado")) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Não encontrado", e.getMessage()));
            } else if (e.getMessage().contains("não pertence")) {
                ctx.status(HttpStatus.FORBIDDEN);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Acesso negado", e.getMessage()));
            } else if (e.getMessage().contains("possui pedidos")) {
                ctx.status(HttpStatus.CONFLICT);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Conflito", e.getMessage()));
            } else {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.header("Cache-Control", "no-store");
                ctx.json(new AuthController.ErrorResponse("Erro ao remover endereço", e.getMessage()));
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.header("Cache-Control", "no-store");
            ctx.json(new AuthController.ErrorResponse("Erro ao remover endereço", e.getMessage()));
        }
    }
    
    // Métodos antigos mantidos para compatibilidade se necessário
    
    /**
     * POST /clientes/{clienteId}/enderecos - Criar novo endereço (compatibilidade)
     */
    public void create(Context ctx) {
        try {
            UUID clienteId = UUID.fromString(ctx.pathParam("clienteId"));
            EnderecoRequestDTO request = ctx.bodyAsClass(EnderecoRequestDTO.class);
            
            EnderecoResponseDTO response = enderecoService.create(clienteId, request);
            
            ctx.status(HttpStatus.CREATED);
            ctx.json(response);
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("ID de cliente inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("Erro ao criar endereço", e.getMessage()));
        }
    }
    
    /**
     * GET /enderecos/{id} - Buscar endereço por ID
     */
    public void findById(Context ctx) {
        try {
            UUID id = UUID.fromString(ctx.pathParam("id"));
            
            var enderecoOpt = enderecoService.findById(id);
            if (enderecoOpt.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(new AuthController.ErrorResponse("Endereço não encontrado", "ID: " + id));
                return;
            }
            
            ctx.status(HttpStatus.OK);
            ctx.json(enderecoOpt.get());
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("ID inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar endereço", e.getMessage()));
        }
    }
    
    /**
     * GET /clientes/{clienteId}/enderecos - Listar endereços do cliente
     */
    public void findByCliente(Context ctx) {
        try {
            UUID clienteId = UUID.fromString(ctx.pathParam("clienteId"));
            
            var enderecos = enderecoService.findByCliente(clienteId);
            
            ctx.status(HttpStatus.OK);
            ctx.json(enderecos);
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("ID de cliente inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar endereços", e.getMessage()));
        }
    }
    
    /**
     * GET /enderecos/cidade/{cidade} - Buscar endereços por cidade
     */
    public void findByCidade(Context ctx) {
        try {
            String cidade = ctx.pathParam("cidade");
            
            var enderecos = enderecoService.findByCidade(cidade);
            
            ctx.status(HttpStatus.OK);
            ctx.json(enderecos);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar endereços", e.getMessage()));
        }
    }
    
    /**
     * GET /enderecos/cep/{cep} - Buscar endereços por CEP
     */
    public void findByCep(Context ctx) {
        try {
            String cep = ctx.pathParam("cep");
            
            var enderecos = enderecoService.findByCep(cep);
            
            ctx.status(HttpStatus.OK);
            ctx.json(enderecos);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao buscar endereços", e.getMessage()));
        }
    }
    
    /**
     * PUT /clientes/{clienteId}/enderecos/{enderecoId} - Atualizar endereço
     */
    public void update(Context ctx) {
        try {
            UUID clienteId = UUID.fromString(ctx.pathParam("clienteId"));
            UUID enderecoId = UUID.fromString(ctx.pathParam("enderecoId"));
            EnderecoRequestDTO request = ctx.bodyAsClass(EnderecoRequestDTO.class);
            
            EnderecoResponseDTO response = enderecoService.update(clienteId, enderecoId, request);
            
            ctx.status(HttpStatus.OK);
            ctx.json(response);
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("IDs inválidos", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("Erro ao atualizar endereço", e.getMessage()));
        }
    }
    
    /**
     * DELETE /clientes/{clienteId}/enderecos/{enderecoId} - Remover endereço
     */
    public void delete(Context ctx) {
        try {
            UUID clienteId = UUID.fromString(ctx.pathParam("clienteId"));
            UUID enderecoId = UUID.fromString(ctx.pathParam("enderecoId"));
            
            enderecoService.delete(clienteId, enderecoId);
            
            ctx.status(HttpStatus.NO_CONTENT);
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("IDs inválidos", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("Erro ao remover endereço", e.getMessage()));
        }
    }
    
    /**
     * GET /clientes/{clienteId}/enderecos/count - Contar endereços do cliente
     */
    public void countByCliente(Context ctx) {
        try {
            UUID clienteId = UUID.fromString(ctx.pathParam("clienteId"));
            
            long count = enderecoService.countByCliente(clienteId);
            
            ctx.status(HttpStatus.OK);
            ctx.json(new CategoriaController.CountResponse(count));
            
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new AuthController.ErrorResponse("ID de cliente inválido", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new AuthController.ErrorResponse("Erro ao contar endereços", e.getMessage()));
        }
    }
}