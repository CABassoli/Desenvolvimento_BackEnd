package com.ecommerce.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Usuário do sistema (autenticação e autorização)
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "email", length = 160, nullable = false, unique = true)
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter formato válido")
    @Size(max = 160, message = "Email deve ter no máximo 160 caracteres")
    private String email;
    
    @Column(name = "password_hash", length = 128, nullable = false)
    @NotBlank(message = "Hash da senha é obrigatório")
    private String passwordHash; // SHA-256 with salt, Base64 encoded
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @NotNull(message = "Role é obrigatória")
    private Role role;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "failed_attempts", nullable = false)
    @Min(value = 0, message = "Tentativas falhadas deve ser maior ou igual a 0")
    private Integer failedAttempts = 0;
    
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    /**
     * Verifica se o usuário está bloqueado por muitas tentativas
     */
    public boolean isLocked() {
        return failedAttempts >= 5;
    }
    
    /**
     * Incrementa tentativas de login falhadas
     */
    public void incrementFailedAttempts() {
        this.failedAttempts++;
    }
    
    /**
     * Reseta tentativas de login falhadas
     */
    public void resetFailedAttempts() {
        this.failedAttempts = 0;
    }
}