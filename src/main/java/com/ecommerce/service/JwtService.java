package com.ecommerce.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.ecommerce.domain.Role;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

/**
 * Serviço para geração e validação de tokens JWT
 * Responsável pela segurança e autenticação da aplicação
 */
public class JwtService {
    
    private static final String SECRET = "ecommerce-secret-key-muito-segura-para-desenvolvimento";
    private static final String ISSUER = "ecommerce-api";
    private static final int EXPIRATION_HOURS = 24;
    
    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    
    public JwtService() {
        this.algorithm = Algorithm.HMAC256(SECRET);
        this.verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build();
    }
    
    /**
     * Gera token JWT para usuário
     * 
     * @param userId O ID do usuário
     * @param email O email do usuário
     * @param role O papel/perfil do usuário
     * @return Token JWT gerado
     */
    public String generateToken(UUID userId, String email, Role role) {
        LocalDateTime expiration = LocalDateTime.now().plusHours(EXPIRATION_HOURS);
        
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(userId.toString())
                .withClaim("email", email)
                .withClaim("role", role.name())
                .withIssuedAt(new Date())
                .withExpiresAt(Date.from(expiration.atZone(ZoneId.systemDefault()).toInstant()))
                .sign(algorithm);
    }
    
    /**
     * Valida e decodifica token JWT
     * 
     * @param token O token a validar
     * @return Token decodificado
     * @throws JWTVerificationException se o token for inválido
     */
    public DecodedJWT validateToken(String token) throws JWTVerificationException {
        return verifier.verify(token);
    }
    
    /**
     * Extrai ID do usuário do token
     * 
     * @param token O token JWT
     * @return ID do usuário
     */
    public UUID extractUserId(String token) {
        DecodedJWT jwt = validateToken(token);
        return UUID.fromString(jwt.getSubject());
    }
    
    /**
     * Extrai email do usuário do token
     * 
     * @param token O token JWT
     * @return Email do usuário
     */
    public String extractEmail(String token) {
        DecodedJWT jwt = validateToken(token);
        return jwt.getClaim("email").asString();
    }
    
    /**
     * Extrai papel/perfil do usuário do token
     * 
     * @param token O token JWT
     * @return Papel do usuário
     */
    public Role extractRole(String token) {
        DecodedJWT jwt = validateToken(token);
        return Role.valueOf(jwt.getClaim("role").asString());
    }
    
    /**
     * Verifica se o token está expirado
     * 
     * @param token O token JWT
     * @return true se expirado, false caso contrário
     */
    public boolean isTokenExpired(String token) {
        try {
            DecodedJWT jwt = validateToken(token);
            return jwt.getExpiresAt().before(new Date());
        } catch (JWTVerificationException e) {
            return true;
        }
    }
}