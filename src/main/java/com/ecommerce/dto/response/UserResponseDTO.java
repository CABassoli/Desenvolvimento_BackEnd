package com.ecommerce.dto.response;

import com.ecommerce.domain.Role;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para resposta com dados do usuário
 */
@Data
public class UserResponseDTO {
    
    private UUID id;
    private String email;
    private Role role;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}