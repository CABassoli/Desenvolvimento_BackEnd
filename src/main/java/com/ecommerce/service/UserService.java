package com.ecommerce.service;

import com.ecommerce.config.DatabaseConfig;
import com.ecommerce.domain.Cliente;
import com.ecommerce.domain.Role;
import com.ecommerce.domain.UserModel;
import com.ecommerce.dto.request.LoginRequestDTO;
import com.ecommerce.dto.response.LoginResponseDTO;
import com.ecommerce.dto.response.UserResponseDTO;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.repository.ClienteRepository;
import com.ecommerce.repository.UserRepository;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço para gerenciamento de usuários e autenticação
 */
public class UserService {
    
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final ClienteRepository clienteRepository;
    
    public UserService(UserRepository userRepository, UserMapper userMapper, 
                      JwtService jwtService, ClienteRepository clienteRepository) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.jwtService = jwtService;
        this.clienteRepository = clienteRepository;
    }
    
    /**
     * Autentica usuário e retorna token JWT
     */
    public LoginResponseDTO authenticate(LoginRequestDTO loginRequest) {
        System.out.println("🚀 AUTHENTICATE DEBUG - Starting authentication for: " + loginRequest.getEmail());
        // Busca usuário por email
        Optional<UserModel> userOpt = userRepository.findByEmail(loginRequest.getEmail());
        if (userOpt.isEmpty()) {
            System.out.println("❌ AUTHENTICATE DEBUG - User not found: " + loginRequest.getEmail());
            throw new RuntimeException("Credenciais inválidas");
        }
        System.out.println("✅ AUTHENTICATE DEBUG - User found: " + loginRequest.getEmail());
        
        UserModel user = userOpt.get();
        
        // Verifica se usuário está ativo
        if (!user.getIsActive()) {
            throw new RuntimeException("Usuário inativo");
        }
        
        // Verifica tentativas falidas
        if (user.getFailedAttempts() >= 5) {
            throw new RuntimeException("Usuário bloqueado por excesso de tentativas");
        }
        
        // Verifica senha
        System.out.println("🔐 AUTHENTICATE DEBUG - About to verify password for: " + user.getEmail());
        System.out.println("🔐 AUTHENTICATE DEBUG - Stored hash length: " + user.getPasswordHash().length());
        if (!verifyPassword(loginRequest.getPassword(), user.getPasswordHash())) {
            System.out.println("❌ AUTHENTICATE DEBUG - Password verification FAILED for: " + user.getEmail());
            // Incrementa tentativas falhadas
            user.setFailedAttempts(user.getFailedAttempts() + 1);
            userRepository.save(user);
            throw new RuntimeException("Credenciais inválidas");
        }
        System.out.println("✅ AUTHENTICATE DEBUG - Password verification SUCCESS for: " + user.getEmail());
        
        // Login bem-sucedido - reseta tentativas e atualiza último login
        user.setFailedAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Gera token JWT
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        
        LoginResponseDTO response = new LoginResponseDTO();
        response.setToken(token);
        response.setUser(userMapper.toResponseDTO(user));
        return response;
    }
    
    /**
     * Cria novo usuário
     */
    public UserResponseDTO createUser(String email, String password, String role) {
        System.out.println("🛠️ CREATEUSER DEBUG - Starting user creation for: " + email);
        
        // Usa os repositórios injetados para evitar problemas de isolamento de transação
        System.out.println("🔄 CREATEUSER DEBUG - Using injected repositories for consistent transaction management");
        
        // Verifica se email já existe
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email já cadastrado");
        }
        
        // Cria novo usuário
        UserModel user = new UserModel();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPasswordHash(hashPassword(password));
        user.setRole(Role.valueOf(role.toUpperCase()));
        user.setIsActive(true);
        user.setFailedAttempts(0);
        user.setCreatedAt(LocalDateTime.now());
        
        System.out.println("💾 CREATEUSER DEBUG - About to save user with hash length: " + user.getPasswordHash().length());
        UserModel savedUser = userRepository.save(user);
        
        // Se o role for CUSTOMER, criar automaticamente um Cliente associado
        if (Role.valueOf(role.toUpperCase()) == Role.CUSTOMER) {
            System.out.println("🛍️ CREATEUSER DEBUG - Creating Cliente for CUSTOMER role: " + email);
            
            // Verifica se já não existe um cliente com este email
            if (!clienteRepository.existsByEmail(email)) {
                Cliente cliente = new Cliente();
                cliente.setId(savedUser.getId()); // Usar o mesmo UUID do User para simplificar
                cliente.setEmail(email);
                cliente.setNome(email.split("@")[0]); // Nome temporário baseado no email
                
                Cliente savedCliente = clienteRepository.save(cliente);
                System.out.println("✅ CREATEUSER DEBUG - Cliente created with ID: " + savedCliente.getId());
            } else {
                System.out.println("ℹ️ CREATEUSER DEBUG - Cliente already exists for email: " + email);
            }
        }
        
        System.out.println("✅ CREATEUSER DEBUG - User created successfully for: " + email);
        
        return userMapper.toResponseDTO(savedUser);
    }
    
    /**
     * Busca usuário por ID
     */
    public Optional<UserResponseDTO> findById(UUID id) {
        return userRepository.findById(id)
                .map(userMapper::toResponseDTO);
    }
    
    /**
     * Busca usuário por email
     */
    public Optional<UserResponseDTO> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toResponseDTO);
    }
    
    /**
     * Lista usuários ativos por role
     */
    public List<UserResponseDTO> findActiveByRole(Role role) {
        return userRepository.findActiveByRole(role).stream()
                .map(userMapper::toResponseDTO)
                .toList();
    }
    
    /**
     * Lista todos usuários ativos
     */
    public List<UserResponseDTO> findAllActive() {
        return userRepository.findAllActive().stream()
                .map(userMapper::toResponseDTO)
                .toList();
    }
    
    /**
     * Desativa usuário
     */
    public void deactivateUser(UUID userId) {
        Optional<UserModel> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Usuário não encontrado");
        }
        
        UserModel user = userOpt.get();
        user.setIsActive(false);
        userRepository.save(user);
    }
    
    /**
     * Altera senha do usuário
     */
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        Optional<UserModel> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Usuário não encontrado");
        }
        
        UserModel user = userOpt.get();
        
        // Verifica senha atual
        if (!verifyPassword(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("Senha atual incorreta");
        }
        
        // Atualiza senha
        user.setPasswordHash(hashPassword(newPassword));
        userRepository.save(user);
    }
    
    /**
     * Gera hash da senha com salt
     */
    private String hashPassword(String password) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());
            
            // Combina salt + hash em Base64
            byte[] saltAndHash = new byte[salt.length + hashedPassword.length];
            System.arraycopy(salt, 0, saltAndHash, 0, salt.length);
            System.arraycopy(hashedPassword, 0, saltAndHash, salt.length, hashedPassword.length);
            
            return Base64.getEncoder().encodeToString(saltAndHash);
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao gerar hash da senha", e);
        }
    }
    
    /**
     * Verifica se senha corresponde ao hash
     */
    private boolean verifyPassword(String password, String storedHash) {
        try {
            System.out.println("DEBUG - Stored hash length: " + storedHash.length());
            System.out.println("DEBUG - Stored hash: " + storedHash);
            
            byte[] saltAndHash = Base64.getDecoder().decode(storedHash);
            System.out.println("DEBUG - Decoded bytes length: " + saltAndHash.length);
            
            // Verifica se tem o tamanho esperado (16 bytes salt + 32 bytes hash = 48 bytes)
            if (saltAndHash.length != 48) {
                System.out.println("ERROR - Stored hash has incorrect length: " + saltAndHash.length + " (expected 48)");
                return false;
            }
            
            // Extrai salt (primeiros 16 bytes)
            byte[] salt = new byte[16];
            System.arraycopy(saltAndHash, 0, salt, 0, 16);
            
            // Extrai hash (resto dos bytes)
            byte[] hash = new byte[saltAndHash.length - 16];
            System.arraycopy(saltAndHash, 16, hash, 0, hash.length);
            
            // Gera hash da senha fornecida com o mesmo salt
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] testHash = md.digest(password.getBytes());
            
            // Compara hashes
            boolean result = MessageDigest.isEqual(hash, testHash);
            System.out.println("DEBUG - Password verification result: " + result);
            return result;
            
        } catch (Exception e) {
            System.out.println("ERROR - Exception in verifyPassword: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}