package com.ecommerce.controller;

import com.ecommerce.dto.request.LoginRequestDTO;
import com.ecommerce.dto.response.LoginResponseDTO;
import com.ecommerce.dto.response.UserResponseDTO;
import com.ecommerce.service.UserService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

/**
 * Controller para autenticação de usuários
 */
public class AuthController {
    
    private final UserService userService;
    
    public AuthController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * POST /auth/login - Autentica usuário
     */
    public void login(Context ctx) {
        try {
            LoginRequestDTO loginRequest = ctx.bodyAsClass(LoginRequestDTO.class);
            
            LoginResponseDTO response = userService.authenticate(loginRequest);
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(response);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.UNAUTHORIZED);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new ErrorResponse("Erro de autenticação", e.getMessage()));
        }
    }
    
    /**
     * POST /auth/register - Registra novo usuário
     */
    public void register(Context ctx) {
        try {
            RegisterRequest request = ctx.bodyAsClass(RegisterRequest.class);
            
            UserResponseDTO response = userService.createUser(
                request.getEmail(), 
                request.getPassword(), 
                request.getRole() != null ? request.getRole() : "CUSTOMER"
            );
            
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
            ctx.json(new ErrorResponse("Erro no registro", e.getMessage()));
        }
    }
    
    /**
     * GET /auth/profile - Retorna perfil do usuário autenticado
     */
    public void getProfile(Context ctx) {
        try {
            String userId = ctx.attribute("userId"); // Será preenchido pelo middleware de auth
            
            if (userId == null) {
                ctx.status(HttpStatus.UNAUTHORIZED);
                ctx.json(new ErrorResponse("Não autorizado", "Token JWT inválido"));
                return;
            }
            
            var userOpt = userService.findById(java.util.UUID.fromString(userId));
            if (userOpt.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
                ctx.header("Pragma", "no-cache");
                ctx.header("Expires", "0");
                ctx.json(new ErrorResponse("Usuário não encontrado", "ID inválido"));
                return;
            }
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(userOpt.get());
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }
    
    /**
     * GET /auth/me - Retorna dados do usuário autenticado
     */
    public void getMe(Context ctx) {
        try {
            String userId = ctx.attribute("userId");
            String userEmail = ctx.attribute("userEmail");
            String userRole = ctx.attribute("userRole");
            
            if (userId == null) {
                ctx.status(HttpStatus.UNAUTHORIZED);
                ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
                ctx.header("Pragma", "no-cache");
                ctx.header("Expires", "0");
                ctx.json(new ErrorResponse("Não autenticado", "Token inválido ou expirado"));
                return;
            }
            
            java.util.Map<String, Object> user = new java.util.HashMap<>();
            user.put("id", userId);
            user.put("email", userEmail);
            user.put("role", userRole);
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(user);
            
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }
    
    /**
     * PUT /auth/change-password - Altera senha do usuário
     */
    public void changePassword(Context ctx) {
        try {
            String userId = ctx.attribute("userId");
            
            if (userId == null) {
                ctx.status(HttpStatus.UNAUTHORIZED);
                ctx.json(new ErrorResponse("Não autorizado", "Token JWT inválido"));
                return;
            }
            
            ChangePasswordRequest request = ctx.bodyAsClass(ChangePasswordRequest.class);
            
            userService.changePassword(
                java.util.UUID.fromString(userId),
                request.getCurrentPassword(),
                request.getNewPassword()
            );
            
            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new SuccessResponse("Senha alterada com sucesso"));
            
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
            ctx.header("Expires", "0");
            ctx.json(new ErrorResponse("Erro ao alterar senha", e.getMessage()));
        }
    }
    
    // Classes auxiliares para requests específicos do controller
    
    public static class RegisterRequest {
        private String email;
        private String password;
        private String role;
        
        // Getters e setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
    
    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
        
        // Getters e setters
        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
    
    // Classes para respostas padrão
    
    public static class ErrorResponse {
        private String error;
        private String message;
        
        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
        
        // Getters
        public String getError() { return error; }
        public String getMessage() { return message; }
    }
    
    public static class SuccessResponse {
        private String message;
        
        public SuccessResponse(String message) {
            this.message = message;
        }
        
        // Getter
        public String getMessage() { return message; }
    }
}