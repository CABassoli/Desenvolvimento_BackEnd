package com.ecommerce.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO para resposta de login bem-sucedido
 */
@Data
public class LoginResponseDTO {
    
    private String token;
    private String tokenType = "Bearer";
    private LocalDateTime expiresAt;
    private UserResponseDTO user;
}