package com.ecommerce.service;

import com.ecommerce.domain.Cliente;
import com.ecommerce.dto.request.ClienteRequestDTO;
import com.ecommerce.dto.response.ClienteResponseDTO;
import com.ecommerce.mapper.ClienteMapper;
import com.ecommerce.repository.ClienteRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço para gerenciamento de clientes
 */
public class ClienteService {
    
    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;
    
    public ClienteService(ClienteRepository clienteRepository,
                         ClienteMapper clienteMapper) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
    }
    
    /**
     * Cria novo cliente
     */
    public ClienteResponseDTO create(ClienteRequestDTO requestDTO) {
        // Verifica se email já existe
        if (clienteRepository.existsByEmail(requestDTO.getEmail())) {
            throw new RuntimeException("Já existe um cliente com este email");
        }
        
        // Valida dados básicos
        if (requestDTO.getNome() == null || requestDTO.getNome().trim().isEmpty()) {
            throw new RuntimeException("Nome é obrigatório");
        }
        
        if (requestDTO.getEmail() == null || requestDTO.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email é obrigatório");
        }
        
        // Cria novo cliente
        Cliente cliente = clienteMapper.toEntity(requestDTO);
        cliente.setId(UUID.randomUUID());
        
        Cliente savedCliente = clienteRepository.save(cliente);
        
        return clienteMapper.toResponseDTO(savedCliente);
    }
    
    /**
     * Busca cliente por ID
     */
    public Optional<ClienteResponseDTO> findById(UUID id) {
        return clienteRepository.findById(id)
                .map(clienteMapper::toResponseDTO);
    }
    
    /**
     * Busca cliente por email
     */
    public Optional<ClienteResponseDTO> findByEmail(String email) {
        return clienteRepository.findByEmail(email)
                .map(clienteMapper::toResponseDTO);
    }
    
    /**
     * Busca cliente com endereços carregados
     */
    public Optional<ClienteResponseDTO> findByIdWithEnderecos(UUID id) {
        return clienteRepository.findByIdWithEnderecos(id)
                .map(clienteMapper::toResponseDTO);
    }
    
    /**
     * Busca cliente com carrinho carregado
     */
    public Optional<ClienteResponseDTO> findByIdWithCarrinho(UUID id) {
        return clienteRepository.findByIdWithCarrinho(id)
                .map(clienteMapper::toResponseDTO);
    }
    
    /**
     * Busca clientes por nome (busca parcial)
     */
    public List<ClienteResponseDTO> findByNome(String nome) {
        return clienteRepository.findByNomeContaining(nome).stream()
                .map(clienteMapper::toResponseDTO)
                .toList();
    }
    
    /**
     * Lista todos os clientes
     */
    public List<ClienteResponseDTO> findAll() {
        return clienteRepository.findAll().stream()
                .map(clienteMapper::toResponseDTO)
                .toList();
    }
    
    /**
     * Atualiza cliente
     */
    public ClienteResponseDTO update(UUID id, ClienteRequestDTO requestDTO) {
        Optional<Cliente> clienteOpt = clienteRepository.findById(id);
        if (clienteOpt.isEmpty()) {
            throw new RuntimeException("Cliente não encontrado");
        }
        
        Cliente cliente = clienteOpt.get();
        
        // Verifica se novo email já existe em outro cliente
        Optional<Cliente> existingCliente = clienteRepository.findByEmail(requestDTO.getEmail());
        if (existingCliente.isPresent() && !existingCliente.get().getId().equals(id)) {
            throw new RuntimeException("Já existe um cliente com este email");
        }
        
        // Valida dados básicos
        if (requestDTO.getNome() == null || requestDTO.getNome().trim().isEmpty()) {
            throw new RuntimeException("Nome é obrigatório");
        }
        
        if (requestDTO.getEmail() == null || requestDTO.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email é obrigatório");
        }
        
        // Atualiza dados
        clienteMapper.updateEntity(requestDTO, cliente);
        
        Cliente savedCliente = clienteRepository.save(cliente);
        
        return clienteMapper.toResponseDTO(savedCliente);
    }
    
    /**
     * Remove cliente
     */
    public void delete(UUID id) {
        Optional<Cliente> clienteOpt = clienteRepository.findById(id);
        if (clienteOpt.isEmpty()) {
            throw new RuntimeException("Cliente não encontrado");
        }
        
        // TODO: Verificar se cliente possui pedidos ou carrinho antes de excluir
        
        clienteRepository.deleteById(id);
    }
    
    /**
     * Conta total de clientes
     */
    public long count() {
        return clienteRepository.count();
    }
    
    /**
     * Verifica se cliente existe
     */
    public boolean exists(UUID id) {
        return clienteRepository.findById(id).isPresent();
    }
    
    /**
     * Verifica se email já está em uso
     */
    public boolean existsByEmail(String email) {
        return clienteRepository.existsByEmail(email);
    }
}