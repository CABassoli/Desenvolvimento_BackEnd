package com.ecommerce.service;

import com.ecommerce.domain.Carrinho;
import com.ecommerce.domain.Cliente;
import com.ecommerce.domain.ItemCarrinho;
import com.ecommerce.domain.Produto;
import com.ecommerce.domain.UserModel;
import com.ecommerce.dto.request.ItemCarrinhoRequestDTO;
import com.ecommerce.dto.response.CarrinhoResponseDTO;
import com.ecommerce.dto.response.ItemCarrinhoResponseDTO;
import com.ecommerce.mapper.CarrinhoMapper;
import com.ecommerce.mapper.ItemCarrinhoMapper;
import com.ecommerce.repository.CarrinhoRepository;
import com.ecommerce.repository.ClienteRepository;
import com.ecommerce.repository.ItemCarrinhoRepository;
import com.ecommerce.repository.ProdutoRepository;
import com.ecommerce.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço para gerenciamento do carrinho de compras
 * Responsável por adicionar, remover e gerenciar itens do carrinho
 */
public class CarrinhoService {
    
    private final CarrinhoRepository carrinhoRepository;
    private final ItemCarrinhoRepository itemCarrinhoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final UserRepository userRepository;
    private final CarrinhoMapper carrinhoMapper;
    private final ItemCarrinhoMapper itemCarrinhoMapper;
    
    public CarrinhoService(CarrinhoRepository carrinhoRepository,
                          ItemCarrinhoRepository itemCarrinhoRepository,
                          ClienteRepository clienteRepository,
                          ProdutoRepository produtoRepository,
                          UserRepository userRepository,
                          CarrinhoMapper carrinhoMapper,
                          ItemCarrinhoMapper itemCarrinhoMapper) {
        this.carrinhoRepository = carrinhoRepository;
        this.itemCarrinhoRepository = itemCarrinhoRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.userRepository = userRepository;
        this.carrinhoMapper = carrinhoMapper;
        this.itemCarrinhoMapper = itemCarrinhoMapper;
    }
    
    /**
     * Busca ou cria carrinho do cliente (usando ID efetivo do cliente)
     * 
     * @param clienteId O ID do cliente
     * @return O carrinho do cliente
     */
    public CarrinhoResponseDTO getOrCreateCarrinho(UUID clienteId) {
        return getOrCreateCarrinhoInternal(clienteId);
    }
    
    /**
     * Busca ou cria carrinho por userId - sempre retorna um carrinho válido
     * 
     * @param userId O ID do usuário
     * @return O carrinho do usuário
     */
    public CarrinhoResponseDTO findOrCreateByUserId(UUID userId) {
        return getOrCreateCarrinhoInternal(userId);
    }
    
    /**
     * Resolve Cliente baseado no userId, garantindo sempre o mesmo Cliente por usuário.
     * Sempre resolve User ID → User → Cliente por email.
     * 
     * @param userId O ID do usuário
     * @return O cliente correspondente ao usuário
     * @throws RuntimeException se o usuário não for encontrado
     */
    private Cliente resolveCliente(UUID userId) {
        // Buscar usuário primeiro
        Optional<UserModel> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Usuário não encontrado");
        }
        
        UserModel user = userOpt.get();
        
        // Buscar cliente existente por email (garante unicidade)
        Optional<Cliente> clientePorEmailOpt = clienteRepository.findByEmail(user.getEmail());
        if (clientePorEmailOpt.isPresent()) {
            // Para evitar entidade desanexada, buscar novamente pelo ID no contexto atual
            Cliente clienteExistente = clientePorEmailOpt.get();
            return clienteRepository.findById(clienteExistente.getId())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        }
        
        // Criar novo cliente (ID será gerado automaticamente)
        Cliente cliente = new Cliente();
        cliente.setEmail(user.getEmail());
        cliente.setNome(user.getEmail().split("@")[0]);
        
        Cliente savedCliente = clienteRepository.save(cliente);
        
        // Para garantir que está no contexto correto, buscar novamente pelo ID
        return clienteRepository.findById(savedCliente.getId())
            .orElseThrow(() -> new RuntimeException("Erro ao criar cliente"));
    }
    
    /**
     * Adiciona item ao carrinho (método legado)
     * 
     * @param clienteId O ID do cliente
     * @param requestDTO Os dados do item a adicionar
     * @return O item adicionado
     * @throws RuntimeException se o produto não existir ou quantidade inválida
     */
    public ItemCarrinhoResponseDTO addItem(UUID clienteId, ItemCarrinhoRequestDTO requestDTO) {
        // Valida a quantidade
        if (requestDTO.getQuantidade() <= 0) {
            throw new RuntimeException("Quantidade deve ser maior que zero");
        }
        
        // Busca o produto
        Optional<Produto> produtoOpt = produtoRepository.findById(requestDTO.getProdutoId());
        if (produtoOpt.isEmpty()) {
            throw new RuntimeException("Produto não encontrado");
        }
        
        Produto produto = produtoOpt.get();
        
        // Resolve o cliente efetivo e busca/cria carrinho
        Cliente cliente = resolveCliente(clienteId);
        UUID effectiveClienteId = cliente.getId();
        
        System.out.println("🛒 DEBUG addItem - UserId: " + clienteId + ", ClienteId: " + effectiveClienteId);
        
        // Busca carrinho existente usando o ID do cliente resolvido
        Optional<Carrinho> carrinhoOpt = carrinhoRepository.findByClienteId(effectiveClienteId);
        Carrinho carrinho;
        
        if (carrinhoOpt.isEmpty()) {
            // Cria novo carrinho
            carrinho = new Carrinho();
            carrinho.setCliente(cliente);
            carrinho = carrinhoRepository.save(carrinho);
            System.out.println("🆕 DEBUG: Novo carrinho criado: " + carrinho.getId());
        } else {
            carrinho = carrinhoOpt.get();
            System.out.println("✅ DEBUG: Carrinho existente encontrado: " + carrinho.getId());
        }
        
        // Verifica se item já existe no carrinho
        Optional<ItemCarrinho> itemExistente = itemCarrinhoRepository
                .findByCarrinhoIdAndProdutoId(carrinho.getId(), produto.getId());
        
        ItemCarrinho item;
        if (itemExistente.isPresent()) {
            // Atualiza quantidade do item existente
            item = itemExistente.get();
            item.setQuantidade(item.getQuantidade() + requestDTO.getQuantidade());
        } else {
            // Cria novo item
            item = new ItemCarrinho();
            item.setCarrinho(carrinho);
            item.setProduto(produto);
            item.setQuantidade(requestDTO.getQuantidade());
        }
        
        ItemCarrinho savedItem = itemCarrinhoRepository.save(item);
        
        return itemCarrinhoMapper.toResponseDTO(savedItem);
    }
    
    /**
     * Adiciona item ao carrinho e retorna carrinho completo
     * @param userId ID do usuário (do JWT)
     * @param produtoId ID do produto
     * @param quantidade Quantidade normalizada (1-999)
     * @return CarrinhoResponseDTO com carrinho atualizado
     */
    public CarrinhoResponseDTO addItem(UUID userId, UUID produtoId, Integer quantidade) {
        // Busca o produto
        Optional<Produto> produtoOpt = produtoRepository.findById(produtoId);
        if (produtoOpt.isEmpty()) {
            throw new IllegalArgumentException("Produto não encontrado");
        }
        
        Produto produto = produtoOpt.get();
        
        // Resolve o cliente efetivo e busca/cria carrinho
        Cliente cliente = resolveCliente(userId);
        UUID effectiveClienteId = cliente.getId();
        
        // Busca carrinho existente usando o ID do cliente resolvido
        Optional<Carrinho> carrinhoOpt = carrinhoRepository.findByClienteId(effectiveClienteId);
        Carrinho carrinho;
        
        if (carrinhoOpt.isEmpty()) {
            // Cria novo carrinho
            carrinho = new Carrinho();
            carrinho.setCliente(cliente);
            carrinho = carrinhoRepository.save(carrinho);
        } else {
            carrinho = carrinhoOpt.get();
        }
        
        // Verifica se item já existe no carrinho
        Optional<ItemCarrinho> itemExistente = itemCarrinhoRepository
                .findByCarrinhoIdAndProdutoId(carrinho.getId(), produto.getId());
        
        ItemCarrinho item;
        if (itemExistente.isPresent()) {
            // Atualiza quantidade do item existente (soma)
            item = itemExistente.get();
            int novaQuantidade = item.getQuantidade() + quantidade;
            if (novaQuantidade > 999) {
                novaQuantidade = 999;
            }
            item.setQuantidade(novaQuantidade);
        } else {
            // Cria novo item
            item = new ItemCarrinho();
            item.setCarrinho(carrinho);
            item.setProduto(produto);
            item.setQuantidade(quantidade);
        }
        
        itemCarrinhoRepository.save(item);
        
        // Retorna carrinho completo atualizado
        return findOrCreateByUserId(userId);
    }
    
    /**
     * Atualiza quantidade do item no carrinho
     */
    public ItemCarrinhoResponseDTO updateItem(UUID clienteId, UUID itemId, ItemCarrinhoRequestDTO requestDTO) {
        // Valida a quantidade
        if (requestDTO.getQuantidade() <= 0) {
            throw new RuntimeException("Quantidade deve ser maior que zero");
        }
        
        // Resolve cliente efetivo
        Cliente cliente = resolveCliente(clienteId);
        UUID effectiveClienteId = cliente.getId();
        
        // Busca item
        Optional<ItemCarrinho> itemOpt = itemCarrinhoRepository.findById(itemId);
        if (itemOpt.isEmpty()) {
            throw new RuntimeException("Item não encontrado");
        }
        
        ItemCarrinho item = itemOpt.get();
        
        // Verifica se item pertence ao cliente (usando ID efetivo)
        if (!item.getCarrinho().getCliente().getId().equals(effectiveClienteId)) {
            throw new RuntimeException("Item não pertence ao cliente");
        }
        
        // Atualiza quantidade
        item.setQuantidade(requestDTO.getQuantidade());
        
        ItemCarrinho savedItem = itemCarrinhoRepository.save(item);
        
        return itemCarrinhoMapper.toResponseDTO(savedItem);
    }
    
    
    /**
     * Remove item do carrinho por produtoId
     * @param userId ID do usuário (do JWT)
     * @param produtoId ID do produto a remover
     */
    public void removeItem(UUID userId, UUID produtoId) {
        // Resolve cliente efetivo
        Cliente cliente = resolveCliente(userId);
        UUID effectiveClienteId = cliente.getId();
        
        // Busca carrinho do cliente
        Optional<Carrinho> carrinhoOpt = carrinhoRepository.findByClienteId(effectiveClienteId);
        if (carrinhoOpt.isPresent()) {
            Carrinho carrinho = carrinhoOpt.get();
            
            // Busca item no carrinho pelo produtoId
            Optional<ItemCarrinho> itemOpt = itemCarrinhoRepository
                    .findByCarrinhoIdAndProdutoId(carrinho.getId(), produtoId);
            
            if (itemOpt.isPresent()) {
                // Remove o item
                itemCarrinhoRepository.deleteById(itemOpt.get().getId());
            }
        }
    }
    
    /**
     * Limpa carrinho do cliente
     */
    public void clearCarrinho(UUID clienteId) {
        // Resolve cliente efetivo
        Cliente cliente = resolveCliente(clienteId);
        UUID effectiveClienteId = cliente.getId();
        
        Optional<Carrinho> carrinhoOpt = carrinhoRepository.findByClienteId(effectiveClienteId);
        if (carrinhoOpt.isPresent()) {
            // Remove todos os itens do carrinho
            itemCarrinhoRepository.deleteByCarrinhoId(carrinhoOpt.get().getId());
        }
    }
    
    /**
     * Calcula valor total do carrinho
     */
    public BigDecimal calcularValorTotal(UUID clienteId) {
        try {
            // Resolve cliente efetivo (pode falhar para usuários novos)
            Cliente cliente = resolveCliente(clienteId);
            UUID effectiveClienteId = cliente.getId();
            
            Optional<Carrinho> carrinhoOpt = carrinhoRepository.findByClienteIdWithItens(effectiveClienteId);
            if (carrinhoOpt.isEmpty()) {
                return BigDecimal.ZERO;
            }
            
            Carrinho carrinho = carrinhoOpt.get();
            
            // Verifica se há itens no carrinho (evita NullPointerException)
            if (carrinho.getItens() == null || carrinho.getItens().isEmpty()) {
                return BigDecimal.ZERO;
            }
            
            return carrinho.getItens().stream()
                    .map(item -> item.getProduto().getPreco().multiply(BigDecimal.valueOf(item.getQuantidade())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (RuntimeException e) {
            // Para usuários novos sem Cliente, retorna zero (carrinho vazio)
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Conta total de itens no carrinho
     */
    public int contarItens(UUID clienteId) {
        try {
            // Resolve cliente efetivo (pode falhar para usuários novos)
            Cliente cliente = resolveCliente(clienteId);
            UUID effectiveClienteId = cliente.getId();
            
            Optional<Carrinho> carrinhoOpt = carrinhoRepository.findByClienteIdWithItens(effectiveClienteId);
            if (carrinhoOpt.isEmpty()) {
                return 0;
            }
            
            Carrinho carrinho = carrinhoOpt.get();
            if (carrinho.getItens() == null || carrinho.getItens().isEmpty()) {
                return 0;
            }
            
            return carrinho.getItens().stream()
                    .mapToInt(ItemCarrinho::getQuantidade)
                    .sum();
        } catch (RuntimeException e) {
            // Para usuários novos sem Cliente, retorna zero (carrinho vazio)
            return 0;
        }
    }
    
    /**
     * Lista itens do carrinho (carregando produtos para mapeamento correto)
     */
    public List<ItemCarrinhoResponseDTO> getItens(UUID clienteId) {
        try {
            // Resolve cliente efetivo (pode falhar para usuários novos)
            Cliente cliente = resolveCliente(clienteId);
            UUID effectiveClienteId = cliente.getId();
            
            Optional<Carrinho> carrinhoOpt = carrinhoRepository.findByClienteIdWithItens(effectiveClienteId);
            if (carrinhoOpt.isEmpty()) {
                return List.of();
            }
            
            Carrinho carrinho = carrinhoOpt.get();
            
            // Verifica se há itens no carrinho (evita NullPointerException)
            if (carrinho.getItens() == null || carrinho.getItens().isEmpty()) {
                return List.of();
            }
            
            return carrinho.getItens().stream()
                    .map(itemCarrinhoMapper::toResponseDTO)
                    .toList();
        } catch (RuntimeException e) {
            // Para usuários novos sem Cliente, retorna lista vazia (carrinho vazio)
            return List.of();
        }
    }
    
    /**
     * Método interno para buscar ou criar carrinho e retornar DTO completo
     */
    private CarrinhoResponseDTO getOrCreateCarrinhoInternal(UUID clienteId) {
        // Resolve cliente efetivo (pode criar Cliente baseado em User se necessário)
        Cliente cliente = resolveCliente(clienteId);
        UUID effectiveClienteId = cliente.getId();
        
        // Busca carrinho existente usando o ID do cliente resolvido
        Optional<Carrinho> carrinhoOpt = carrinhoRepository.findByClienteId(effectiveClienteId);
        Carrinho carrinho;
        
        if (carrinhoOpt.isEmpty()) {
            // Cria novo carrinho
            carrinho = new Carrinho();

            carrinho.setCliente(cliente);
            carrinho = carrinhoRepository.save(carrinho);
        } else {
            carrinho = carrinhoOpt.get();
        }
        
        // Recarrega carrinho com itens e produtos para garantir que estão incluídos no response
        Carrinho carrinhoComItens = carrinhoRepository.findByIdWithItens(carrinho.getId())
                .orElse(carrinho);
        
        // Se não conseguiu carregar itens, inicializa lista vazia
        if (carrinhoComItens.getItens() == null) {
            System.out.println("🚨 DEBUG: findByIdWithItens retornou carrinho sem itens! Inicializando lista vazia...");
            carrinhoComItens.setItens(new java.util.ArrayList<>());
        }
        
        // Debug: verificar estado do carrinho antes do mapeamento
        System.out.println("🔍 DEBUG: Carrinho antes do mapeamento:");
        System.out.println("  - ID: " + carrinhoComItens.getId());
        System.out.println("  - Itens: " + (carrinhoComItens.getItens() != null ? 
            carrinhoComItens.getItens().size() + " itens" : "NULL"));
        if (carrinhoComItens.getItens() != null && !carrinhoComItens.getItens().isEmpty()) {
            System.out.println("  - Primeiro item: " + carrinhoComItens.getItens().get(0).getProduto().getNome());
        }
        
        CarrinhoResponseDTO response = carrinhoMapper.toResponseDTO(carrinhoComItens);
        
        // Debug: verificar estado do DTO após mapeamento
        System.out.println("📋 DEBUG: DTO após mapeamento:");
        System.out.println("  - ID: " + response.getId());
        System.out.println("  - Itens: " + (response.getItens() != null ? 
            response.getItens().size() + " itens" : "NULL"));
        System.out.println("  - ValorTotal: " + response.getValorTotal());
        System.out.println("  - TotalItens: " + response.getTotalItens());
        
        return response;
    }
}