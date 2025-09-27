package com.ecommerce.security;

import com.ecommerce.repository.PedidoRepository;
import com.ecommerce.repository.ClienteRepository;
import com.ecommerce.repository.EnderecoRepository;
import com.ecommerce.repository.CarrinhoRepository;
import com.ecommerce.domain.Pedido;
import com.ecommerce.domain.Endereco;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.Map;
import java.util.UUID;
import java.util.Optional;

/**
 * Validador de propriedade de recursos para garantir que usuários só acessem seus próprios dados.
 * Implementa verificações de segurança para isolamento de dados entre clientes.
 */
public class OwnershipValidator {
    
    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final EnderecoRepository enderecoRepository;
    private final CarrinhoRepository carrinhoRepository;
    
    public OwnershipValidator(PedidoRepository pedidoRepository, ClienteRepository clienteRepository, 
                             EnderecoRepository enderecoRepository, CarrinhoRepository carrinhoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.enderecoRepository = enderecoRepository;
        this.carrinhoRepository = carrinhoRepository;
    }
    
    /**
     * Valida se o usuário autenticado pode acessar o pedido especificado
     */
    public boolean canAccessPedido(Context ctx, UUID pedidoId) {
        String userId = ctx.attribute("userId");
        String userRole = ctx.attribute("userRole");
        
        // MANAGER pode acessar qualquer pedido
        if ("MANAGER".equals(userRole)) {
            return true;
        }
        
        // CUSTOMER só pode acessar seus próprios pedidos
        if ("CUSTOMER".equals(userRole)) {
            try {
                Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoId);
                if (pedidoOpt.isPresent()) {
                    return pedidoOpt.get().getCliente().getId().toString().equals(userId);
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Valida se o usuário autenticado pode acessar o cliente especificado
     */
    public boolean canAccessCliente(Context ctx, UUID clienteId) {
        String userId = ctx.attribute("userId");
        String userRole = ctx.attribute("userRole");
        
        // MANAGER pode acessar qualquer cliente
        if ("MANAGER".equals(userRole)) {
            return true;
        }
        
        // CUSTOMER só pode acessar seus próprios dados
        if ("CUSTOMER".equals(userRole)) {
            return clienteId.toString().equals(userId);
        }
        
        return false;
    }
    
    /**
     * Valida se o usuário autenticado pode acessar o endereço especificado
     */
    public boolean canAccessEndereco(Context ctx, UUID enderecoId) {
        String userId = ctx.attribute("userId");
        String userRole = ctx.attribute("userRole");
        
        // MANAGER pode acessar qualquer endereço
        if ("MANAGER".equals(userRole)) {
            return true;
        }
        
        // CUSTOMER só pode acessar endereços próprios
        if ("CUSTOMER".equals(userRole)) {
            try {
                Optional<Endereco> enderecoOpt = enderecoRepository.findById(enderecoId);
                if (enderecoOpt.isPresent()) {
                    return enderecoOpt.get().getCliente().getId().toString().equals(userId);
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Valida se o usuário autenticado pode acessar o carrinho especificado
     */
    public boolean canAccessCarrinho(Context ctx, UUID clienteId) {
        return canAccessCliente(ctx, clienteId);
    }
    
    /**
     * Middleware para validar propriedade de pedido
     */
    public void validatePedidoOwnership(Context ctx) {
        try {
            // Check authentication first
            String userId = ctx.attribute("userId");
            String userRole = ctx.attribute("userRole");
            
            if (userId == null || userRole == null) {
                ctx.status(HttpStatus.UNAUTHORIZED);
                ctx.json(Map.of(
                    "error", "Não autenticado",
                    "message", "Token de autenticação é obrigatório"
                ));
                return;
            }
            
            UUID pedidoId = UUID.fromString(ctx.pathParam("id"));
            
            if (!canAccessPedido(ctx, pedidoId)) {
                ctx.status(HttpStatus.FORBIDDEN);
                ctx.json(Map.of(
                    "error", "Acesso negado",
                    "message", "Você não tem permissão para acessar este pedido"
                ));
                return;
            }
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(Map.of("error", "ID do pedido inválido"));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(Map.of("error", "Erro interno do servidor"));
        }
    }
    
    /**
     * Middleware para validar propriedade de cliente
     */
    public void validateClienteOwnership(Context ctx) {
        try {
            // Check authentication first
            String userId = ctx.attribute("userId");
            String userRole = ctx.attribute("userRole");
            
            if (userId == null || userRole == null) {
                ctx.status(HttpStatus.UNAUTHORIZED);
                ctx.json(Map.of(
                    "error", "Não autenticado",
                    "message", "Token de autenticação é obrigatório"
                ));
                return;
            }
            
            UUID clienteId = UUID.fromString(ctx.pathParam("clienteId"));
            
            if (!canAccessCliente(ctx, clienteId)) {
                ctx.status(HttpStatus.FORBIDDEN);
                ctx.json(Map.of(
                    "error", "Acesso negado",
                    "message", "Você não tem permissão para acessar este cliente"
                ));
                return;
            }
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(Map.of("error", "ID do cliente inválido"));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(Map.of("error", "Erro interno do servidor"));
        }
    }
    
    /**
     * Middleware para validar propriedade de endereço
     */
    public void validateEnderecoOwnership(Context ctx) {
        try {
            // Check authentication first
            String userId = ctx.attribute("userId");
            String userRole = ctx.attribute("userRole");
            
            if (userId == null || userRole == null) {
                ctx.status(HttpStatus.UNAUTHORIZED);
                ctx.json(Map.of(
                    "error", "Não autenticado",
                    "message", "Token de autenticação é obrigatório"
                ));
                return;
            }
            
            UUID enderecoId = UUID.fromString(ctx.pathParam("enderecoId"));
            
            if (!canAccessEndereco(ctx, enderecoId)) {
                ctx.status(HttpStatus.FORBIDDEN);
                ctx.json(Map.of(
                    "error", "Acesso negado",
                    "message", "Você não tem permissão para acessar este endereço"
                ));
                return;
            }
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(Map.of("error", "ID do endereço inválido"));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(Map.of("error", "Erro interno do servidor"));
        }
    }
}