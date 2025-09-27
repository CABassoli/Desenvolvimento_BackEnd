package com.ecommerce.service;

import com.ecommerce.domain.Cliente;
import com.ecommerce.domain.Endereco;
import com.ecommerce.domain.Pedido;
import com.ecommerce.domain.UserModel;
import com.ecommerce.dto.request.EnderecoRequestDTO;
import com.ecommerce.dto.response.EnderecoResponseDTO;
import com.ecommerce.mapper.EnderecoMapper;
import com.ecommerce.repository.ClienteRepository;
import com.ecommerce.repository.EnderecoRepository;
import com.ecommerce.repository.PedidoRepository;
import com.ecommerce.repository.UserRepository;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço para gerenciamento de endereços dos clientes
 */
public class EnderecoService {
    
    private final EnderecoRepository enderecoRepository;
    private final ClienteRepository clienteRepository;
    private final UserRepository userRepository;
    private final PedidoRepository pedidoRepository;
    private final EnderecoMapper enderecoMapper;
    
    public EnderecoService(EnderecoRepository enderecoRepository,
                          ClienteRepository clienteRepository,
                          UserRepository userRepository,
                          PedidoRepository pedidoRepository,
                          EnderecoMapper enderecoMapper) {
        this.enderecoRepository = enderecoRepository;
        this.clienteRepository = clienteRepository;
        this.userRepository = userRepository;
        this.pedidoRepository = pedidoRepository;
        this.enderecoMapper = enderecoMapper;
    }
    
    // Constructor for backward compatibility
    public EnderecoService(EnderecoRepository enderecoRepository,
                          ClienteRepository clienteRepository,
                          EnderecoMapper enderecoMapper) {
        this(enderecoRepository, clienteRepository, null, null, enderecoMapper);
    }
    
    /**
     * Resolve Cliente baseado no userId, garantindo sempre o mesmo Cliente por usuário.
     * CORREÇÃO: Sempre resolve User ID → User → Cliente por email.
     */
    private Cliente resolveCliente(UUID userId) {
        System.out.println("🔍 ENDERECO DEBUG - Resolvendo Cliente para userId: " + userId);
        
        // SEMPRE buscar User primeiro para obter o email
        if (userRepository != null) {
            Optional<UserModel> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                UserModel user = userOpt.get();
                System.out.println("✅ ENDERECO DEBUG - User encontrado: " + user.getEmail());
                
                // Buscar Cliente existente por email (garante unicidade)
                Optional<Cliente> clientePorEmailOpt = clienteRepository.findByEmail(user.getEmail());
                if (clientePorEmailOpt.isPresent()) {
                    Cliente cliente = clientePorEmailOpt.get();
                    System.out.println("✅ ENDERECO DEBUG - Cliente encontrado por email com ID: " + cliente.getId());
                    return cliente;
                }
                
                // Se não existe Cliente, criar um novo com ID único
                System.out.println("🛍️ ENDERECO DEBUG - Criando novo Cliente para User: " + user.getEmail());
                Cliente cliente = new Cliente();
                cliente.setId(UUID.randomUUID()); // Gerar novo UUID único para Cliente
                cliente.setEmail(user.getEmail());
                cliente.setNome(user.getEmail().split("@")[0]);
                
                Cliente savedCliente = clienteRepository.save(cliente);
                System.out.println("✅ ENDERECO DEBUG - Cliente criado com ID: " + savedCliente.getId());
                
                return savedCliente;
            } else {
                throw new RuntimeException("Usuário não encontrado com ID: " + userId);
            }
        }
        
        // Fallback: Se não há userRepository (apenas para testes)
        // Tentar buscar Cliente que pode ter sido criado com userId como clienteId (dados legados)
        Optional<Cliente> clienteOpt = clienteRepository.findById(userId);
        if (clienteOpt.isPresent()) {
            System.out.println("⚠️ ENDERECO DEBUG - Cliente legado encontrado com userId como clienteId: " + userId);
            return clienteOpt.get();
        }
        
        throw new RuntimeException("Não foi possível resolver Cliente para userId: " + userId);
    }
    
    /**
     * Lista todos os endereços de um cliente
     */
    public List<EnderecoResponseDTO> listarEnderecos(UUID clienteId) {
        Cliente cliente = resolveCliente(clienteId);
        List<Endereco> enderecos = enderecoRepository.findAllByClienteId(cliente.getId());
        
        return enderecos.stream()
            .map(enderecoMapper::toResponseDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Busca endereço específico do cliente autenticado
     * Retorna erro se endereço não existir ou não pertencer ao cliente
     */
    public EnderecoResponseDTO buscarEnderecoPorIdECliente(UUID clienteId, UUID enderecoId) {
        Cliente cliente = resolveCliente(clienteId);
        
        // Buscar endereço validando propriedade
        Optional<Endereco> enderecoOpt = enderecoRepository.findByIdAndClienteId(enderecoId, cliente.getId());
        
        if (enderecoOpt.isEmpty()) {
            throw new RuntimeException("Endereço não encontrado para este cliente");
        }
        
        return enderecoMapper.toResponseDTO(enderecoOpt.get());
    }
    
    /**
     * Cria um novo endereço para o cliente
     */
    @Transactional
    public EnderecoResponseDTO criarEndereco(UUID clienteId, EnderecoRequestDTO dto) {
        Cliente cliente = resolveCliente(clienteId);
        
        // Valida campos obrigatórios
        if (dto.getRua() == null || dto.getRua().trim().isEmpty()) {
            throw new IllegalArgumentException("Rua é obrigatória");
        }
        if (dto.getNumero() == null || dto.getNumero().trim().isEmpty()) {
            throw new IllegalArgumentException("Número é obrigatório");
        }
        if (dto.getCidade() == null || dto.getCidade().trim().isEmpty()) {
            throw new IllegalArgumentException("Cidade é obrigatória");
        }
        
        // Valida e normaliza CEP
        String cepNormalizado = validarCEP(dto.getCep());
        
        // Se ehPadrao=true, desmarcar outros endereços do cliente
        if (dto.getEhPadrao() != null && dto.getEhPadrao()) {
            enderecoRepository.desmarcarEnderecoPadrao(cliente.getId());
        }
        
        // Cria novo endereço
        Endereco endereco = enderecoMapper.toEntity(dto);
        endereco.setId(UUID.randomUUID());
        endereco.setCliente(cliente);
        endereco.setCep(cepNormalizado);
        endereco.setEhPadrao(dto.getEhPadrao() != null ? dto.getEhPadrao() : false);
        
        // Se for o primeiro endereço do cliente, define como padrão
        long countEnderecos = enderecoRepository.countByCliente(cliente.getId());
        if (countEnderecos == 0) {
            endereco.setEhPadrao(true);
        }
        
        // Define campos obrigatórios se não presentes no DTO
        if (endereco.getBairro() == null || endereco.getBairro().isBlank()) {
            endereco.setBairro("Centro");
        }
        if (endereco.getEstado() == null || endereco.getEstado().isBlank()) {
            endereco.setEstado("SP");
        }
        
        Endereco savedEndereco = enderecoRepository.save(endereco);
        
        return enderecoMapper.toResponseDTO(savedEndereco);
    }
    
    /**
     * Atualiza um endereço existente
     */
    @Transactional
    public EnderecoResponseDTO atualizarEndereco(UUID clienteId, UUID enderecoId, EnderecoRequestDTO dto) {
        Cliente cliente = resolveCliente(clienteId);
        
        Optional<Endereco> enderecoOpt = enderecoRepository.findById(enderecoId);
        if (enderecoOpt.isEmpty()) {
            throw new RuntimeException("Endereço não encontrado");
        }
        
        Endereco endereco = enderecoOpt.get();
        
        // Verifica se endereço pertence ao cliente
        if (!endereco.getCliente().getId().equals(cliente.getId())) {
            throw new RuntimeException("Endereço não pertence ao cliente");
        }
        
        // Valida e normaliza CEP
        String cepNormalizado = validarCEP(dto.getCep());
        
        // Atualiza dados mantendo o estado de endereço padrão
        Boolean ehPadraoAnterior = endereco.getEhPadrao();
        enderecoMapper.updateEntity(dto, endereco);
        endereco.setCep(cepNormalizado);
        endereco.setEhPadrao(ehPadraoAnterior);
        
        // Define campos obrigatórios se não presentes
        if (endereco.getBairro() == null || endereco.getBairro().isBlank()) {
            endereco.setBairro("Centro");
        }
        if (endereco.getEstado() == null || endereco.getEstado().isBlank()) {
            endereco.setEstado("SP");
        }
        
        Endereco updatedEndereco = enderecoRepository.save(endereco);
        
        return enderecoMapper.toResponseDTO(updatedEndereco);
    }
    
    /**
     * Define um endereço como padrão
     */
    @Transactional
    public EnderecoResponseDTO definirEnderecoPadrao(UUID clienteId, UUID enderecoId) {
        Cliente cliente = resolveCliente(clienteId);
        
        Optional<Endereco> enderecoOpt = enderecoRepository.findById(enderecoId);
        if (enderecoOpt.isEmpty()) {
            throw new RuntimeException("Endereço não encontrado");
        }
        
        Endereco endereco = enderecoOpt.get();
        
        // Verifica se endereço pertence ao cliente
        if (!endereco.getCliente().getId().equals(cliente.getId())) {
            throw new RuntimeException("Endereço não pertence ao cliente");
        }
        
        // Remove padrão do endereço atual (se houver)
        Optional<Endereco> enderecoPadraoAtual = enderecoRepository.findByClienteIdAndEhPadrao(cliente.getId(), true);
        if (enderecoPadraoAtual.isPresent()) {
            Endereco enderecoAtual = enderecoPadraoAtual.get();
            enderecoAtual.setEhPadrao(false);
            enderecoRepository.save(enderecoAtual);
        }
        
        // Define novo endereço como padrão
        endereco.setEhPadrao(true);
        Endereco updatedEndereco = enderecoRepository.save(endereco);
        
        return enderecoMapper.toResponseDTO(updatedEndereco);
    }
    
    /**
     * Remove um endereço se não estiver sendo usado por nenhum pedido
     */
    @Transactional
    public void removerEndereco(UUID clienteId, UUID enderecoId) {
        Cliente cliente = resolveCliente(clienteId);
        
        Optional<Endereco> enderecoOpt = enderecoRepository.findById(enderecoId);
        if (enderecoOpt.isEmpty()) {
            throw new RuntimeException("Endereço não encontrado");
        }
        
        Endereco endereco = enderecoOpt.get();
        
        // Verifica se endereço pertence ao cliente
        if (!endereco.getCliente().getId().equals(cliente.getId())) {
            throw new RuntimeException("Endereço não pertence ao cliente");
        }
        
        // Verifica se endereço está sendo usado em algum pedido
        if (pedidoRepository != null) {
            List<Pedido> pedidosComEndereco = pedidoRepository.findAll().stream()
                .filter(p -> p.getEnderecoEntrega() != null && p.getEnderecoEntrega().getId().equals(enderecoId))
                .collect(Collectors.toList());
            
            if (!pedidosComEndereco.isEmpty()) {
                throw new RuntimeException("Endereço está sendo usado em " + pedidosComEndereco.size() + " pedido(s) e não pode ser removido");
            }
        }
        
        // Se é o endereço padrão, define outro como padrão (se houver)
        if (endereco.getEhPadrao()) {
            List<Endereco> outrosEnderecos = enderecoRepository.findByClienteId(cliente.getId()).stream()
                .filter(e -> !e.getId().equals(enderecoId))
                .collect(Collectors.toList());
            
            if (!outrosEnderecos.isEmpty()) {
                Endereco novoEnderecoPadrao = outrosEnderecos.get(0);
                novoEnderecoPadrao.setEhPadrao(true);
                enderecoRepository.save(novoEnderecoPadrao);
            }
        }
        
        enderecoRepository.deleteById(enderecoId);
    }
    
    /**
     * Valida formato do CEP e normaliza para apenas números
     */
    public String validarCEP(String cep) {
        if (cep == null || cep.isBlank()) {
            throw new IllegalArgumentException("CEP é obrigatório");
        }
        
        // Remove todos os caracteres não numéricos
        String cepNormalizado = cep.replaceAll("[^0-9]", "");
        
        // Verifica se tem exatamente 8 dígitos
        if (cepNormalizado.length() != 8) {
            throw new IllegalArgumentException("CEP deve ter exatamente 8 dígitos");
        }
        
        return cepNormalizado;
    }
    
    // Métodos existentes mantidos para compatibilidade
    
    /**
     * Cria novo endereço para cliente (método legado)
     */
    public EnderecoResponseDTO create(UUID clienteId, EnderecoRequestDTO requestDTO) {
        return criarEndereco(clienteId, requestDTO);
    }
    
    /**
     * Busca endereço por ID
     */
    public Optional<EnderecoResponseDTO> findById(UUID id) {
        return enderecoRepository.findById(id)
                .map(enderecoMapper::toResponseDTO);
    }
    
    /**
     * Lista endereços por cliente (método legado)
     */
    public List<EnderecoResponseDTO> findByCliente(UUID clienteId) {
        return listarEnderecos(clienteId);
    }
    
    /**
     * Busca endereços por cidade
     */
    public List<EnderecoResponseDTO> findByCidade(String cidade) {
        return enderecoRepository.findByCidade(cidade).stream()
                .map(enderecoMapper::toResponseDTO)
                .toList();
    }
    
    /**
     * Busca endereços por CEP
     */
    public List<EnderecoResponseDTO> findByCep(String cep) {
        return enderecoRepository.findByCep(cep).stream()
                .map(enderecoMapper::toResponseDTO)
                .toList();
    }
    
    /**
     * Atualiza endereço (método legado)
     */
    public EnderecoResponseDTO update(UUID clienteId, UUID enderecoId, EnderecoRequestDTO requestDTO) {
        return atualizarEndereco(clienteId, enderecoId, requestDTO);
    }
    
    /**
     * Remove endereço (método legado)
     */
    public void delete(UUID clienteId, UUID enderecoId) {
        removerEndereco(clienteId, enderecoId);
    }
    
    /**
     * Conta endereços por cliente
     */
    public long countByCliente(UUID clienteId) {
        return enderecoRepository.countByCliente(clienteId);
    }
    
    /**
     * Verifica se cliente possui endereço específico
     */
    public boolean existsByClienteAndId(UUID clienteId, UUID enderecoId) {
        return enderecoRepository.existsByClienteAndId(clienteId, enderecoId);
    }
    
    /**
     * Valida dados do endereço (método privado mantido para compatibilidade)
     */
    private void validateEnderecoData(EnderecoRequestDTO requestDTO) {
        if (requestDTO.getRua() == null || requestDTO.getRua().trim().isEmpty()) {
            throw new RuntimeException("Rua é obrigatória");
        }
        
        if (requestDTO.getNumero() == null || requestDTO.getNumero().trim().isEmpty()) {
            throw new RuntimeException("Número é obrigatório");
        }
        
        if (requestDTO.getCidade() == null || requestDTO.getCidade().trim().isEmpty()) {
            throw new RuntimeException("Cidade é obrigatória");
        }
        
        if (requestDTO.getCep() == null || requestDTO.getCep().trim().isEmpty()) {
            throw new RuntimeException("CEP é obrigatório");
        }
        
        // Valida formato do CEP usando o método validarCEP
        validarCEP(requestDTO.getCep());
    }
}