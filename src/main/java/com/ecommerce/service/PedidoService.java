package com.ecommerce.service;

import com.ecommerce.domain.*;
import com.ecommerce.dto.ConfirmarPedidoRequestDTO;
import com.ecommerce.dto.ItemPedidoDTO;
import com.ecommerce.dto.PagamentoConfirmacaoDTO;
import com.ecommerce.dto.SimulacaoPagamentoResponseDTO;
import jakarta.transaction.Transactional;
import jakarta.persistence.EntityManager;
import com.ecommerce.dto.request.PedidoRequestDTO;
import com.ecommerce.dto.response.PedidoResponseDTO;
import com.ecommerce.mapper.PedidoMapper;
import com.ecommerce.repository.*;
import com.ecommerce.repository.PedidoItemRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Serviço para gerenciamento de pedidos
 * Responsável por criar, atualizar e consultar pedidos
 */
public class PedidoService {
    
    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final EnderecoRepository enderecoRepository;
    private final CarrinhoRepository carrinhoRepository;
    private final ItemCarrinhoRepository itemCarrinhoRepository;
    private final ItemPedidoRepository itemPedidoRepository;
    private final PedidoItemRepository pedidoItemRepository;
    private final UserRepository userRepository;
    private final PedidoMapper pedidoMapper;
    private final NotificacaoService notificacaoService;
    private final CarrinhoService carrinhoService;
    private final ProdutoRepository produtoRepository;
    private final AtomicInteger sequenciaNumero = new AtomicInteger(1);
    
    public PedidoService(PedidoRepository pedidoRepository,
                        ClienteRepository clienteRepository,
                        EnderecoRepository enderecoRepository,
                        CarrinhoRepository carrinhoRepository,
                        ItemCarrinhoRepository itemCarrinhoRepository,
                        ItemPedidoRepository itemPedidoRepository,
                        PedidoItemRepository pedidoItemRepository,
                        UserRepository userRepository,
                        PedidoMapper pedidoMapper,
                        NotificacaoService notificacaoService,
                        CarrinhoService carrinhoService,
                        ProdutoRepository produtoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.enderecoRepository = enderecoRepository;
        this.carrinhoRepository = carrinhoRepository;
        this.itemCarrinhoRepository = itemCarrinhoRepository;
        this.itemPedidoRepository = itemPedidoRepository;
        this.pedidoItemRepository = pedidoItemRepository;
        this.userRepository = userRepository;
        this.pedidoMapper = pedidoMapper;
        this.notificacaoService = notificacaoService;
        this.carrinhoService = carrinhoService;
        this.produtoRepository = produtoRepository;
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
        System.out.println("🔍 PEDIDO DEBUG - Resolvendo cliente para userId: " + userId);
        
        // Buscar usuário primeiro
        Optional<UserModel> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Usuário não encontrado");
        }
        
        UserModel user = userOpt.get();
        System.out.println("✅ PEDIDO DEBUG - Usuário encontrado: " + user.getEmail());
        
        // Buscar cliente existente por email (garante unicidade)
        Optional<Cliente> clientePorEmailOpt = clienteRepository.findByEmail(user.getEmail());
        if (clientePorEmailOpt.isPresent()) {
            Cliente clienteExistente = clientePorEmailOpt.get();
            System.out.println("✅ PEDIDO DEBUG - Cliente encontrado por email com ID: " + clienteExistente.getId());
            // Para evitar entidade desanexada, buscar novamente pelo ID no contexto atual
            return clienteRepository.findById(clienteExistente.getId())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        }
        
        // Criar novo cliente com UUID único
        System.out.println("🛍️ PEDIDO DEBUG - Criando novo cliente para usuário: " + user.getEmail());
        Cliente cliente = new Cliente();
        cliente.setId(UUID.randomUUID()); // Gerar novo UUID único para cliente
        cliente.setEmail(user.getEmail());
        cliente.setNome(user.getEmail().split("@")[0]);
        
        Cliente savedCliente = clienteRepository.save(cliente);
        System.out.println("✅ PEDIDO DEBUG - Cliente criado com ID: " + savedCliente.getId());
        
        // Para garantir que está no contexto correto, buscar novamente pelo ID
        return clienteRepository.findById(savedCliente.getId())
            .orElseThrow(() -> new RuntimeException("Erro ao criar cliente"));
    }
    
    /**
     * Resolve Cliente baseado no userId usando repositórios transacionais.
     * 
     * @param userId O ID do usuário
     * @param txUserRepository Repositório de usuário transacional
     * @param txClienteRepository Repositório de cliente transacional
     * @return O cliente correspondente ao usuário
     */
    private Cliente resolveClienteTransactional(UUID userId, UserRepository txUserRepository, ClienteRepository txClienteRepository) {
        System.out.println("🔍 PEDIDO TX DEBUG - Resolvendo cliente (transacional) para userId: " + userId);
        
        // Buscar usuário primeiro
        Optional<UserModel> userOpt = txUserRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Usuário não encontrado");
        }
        
        UserModel user = userOpt.get();
        System.out.println("✅ PEDIDO TX DEBUG - Usuário encontrado: " + user.getEmail());
        
        // Buscar cliente existente por email (garante unicidade)
        Optional<Cliente> clientePorEmailOpt = txClienteRepository.findByEmail(user.getEmail());
        if (clientePorEmailOpt.isPresent()) {
            Cliente clienteExistente = clientePorEmailOpt.get();
            System.out.println("✅ PEDIDO TX DEBUG - Cliente encontrado por email com ID: " + clienteExistente.getId());
            return txClienteRepository.findById(clienteExistente.getId())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        }
        
        // Criar novo cliente com UUID único
        System.out.println("🛍️ PEDIDO TX DEBUG - Criando novo cliente para usuário: " + user.getEmail());
        Cliente cliente = new Cliente();
        cliente.setId(UUID.randomUUID()); // Gerar novo UUID único para cliente
        cliente.setEmail(user.getEmail());
        cliente.setNome(user.getEmail().split("@")[0]);
        
        Cliente savedCliente = txClienteRepository.save(cliente);
        System.out.println("✅ PEDIDO TX DEBUG - Cliente criado com ID: " + savedCliente.getId());
        
        return txClienteRepository.findById(savedCliente.getId())
            .orElseThrow(() -> new RuntimeException("Erro ao criar cliente"));
    }
    
    /**
     * Confirma pedido a partir do carrinho do cliente
     * 
     * @param clienteId O ID do cliente
     * @param requestDTO Os dados do pedido
     * @return O pedido confirmado
     * @throws RuntimeException se o carrinho estiver vazio ou endereço não pertencer ao cliente
     */
    @Transactional
    public PedidoResponseDTO confirmarPedido(UUID clienteId, PedidoRequestDTO requestDTO) {
        System.out.println("🔄 PEDIDO DEBUG - Iniciando confirmação de pedido para clienteId: " + clienteId);
        
        if (requestDTO.getIdempotencyKey() != null && !requestDTO.getIdempotencyKey().isBlank()) {
            Optional<Pedido> existingPedido = pedidoRepository.findByIdempotencyKey(requestDTO.getIdempotencyKey());
            if (existingPedido.isPresent()) {
                System.out.println("✨ PEDIDO INFO - Chave de idempotência encontrada, retornando pedido existente: " + existingPedido.get().getId());
                return pedidoMapper.toResponseDTO(existingPedido.get());
            }
        }
        
        // Resolve cliente corretamente (User ID → Cliente por email)
        Cliente cliente = resolveCliente(clienteId);
        System.out.println("✅ PEDIDO DEBUG - Cliente resolvido: " + cliente.getId());
        
        // Verifica se endereço existe e pertence ao cliente
        Endereco endereco = null;
        if (requestDTO.getEnderecoId() != null) {
            Optional<Endereco> enderecoOpt = enderecoRepository.findById(requestDTO.getEnderecoId());
            if (enderecoOpt.isEmpty()) {
                throw new RuntimeException("Endereço não encontrado");
            }
            
            endereco = enderecoOpt.get();
            if (!endereco.getCliente().getId().equals(cliente.getId())) {
                throw new RuntimeException("Endereço não pertence ao cliente");
            }
        }
        
        // Busca carrinho com itens usando o ID do cliente
        Optional<Carrinho> carrinhoOpt = carrinhoRepository.findByClienteIdWithItens(cliente.getId());
        if (carrinhoOpt.isEmpty() || carrinhoOpt.get().getItens().isEmpty()) {
            throw new RuntimeException("Carrinho vazio ou não encontrado");
        }
        
        Carrinho carrinho = carrinhoOpt.get();
        System.out.println("✅ PEDIDO DEBUG - Carrinho encontrado com " + carrinho.getItens().size() + " itens");
        
        // Calcula valor total
        BigDecimal valorTotal = calcularValorTotal(carrinho);
        if (valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Valor do pedido deve ser maior que zero");
        }
        
        // Cria o pedido - ID será gerado automaticamente
        Pedido pedido = new Pedido();
        // ID será gerado automaticamente pelo @GeneratedValue
        pedido.setCliente(cliente);
        pedido.setClienteId(cliente.getId());
        pedido.setEnderecoEntrega(endereco);
        pedido.setDataPedido(LocalDateTime.now());
        pedido.setStatus(StatusPedido.NOVO);
        pedido.setValorTotal(valorTotal);
        pedido.setTotal(valorTotal);
        if (requestDTO.getIdempotencyKey() != null && !requestDTO.getIdempotencyKey().isBlank()) {
            pedido.setIdempotencyKey(requestDTO.getIdempotencyKey());
        }
        
        // Salva o pedido primeiro para obter o ID gerado
        Pedido savedPedido = pedidoRepository.save(pedido);
        System.out.println("💾 PEDIDO DEBUG - Pedido salvo com ID gerado: " + savedPedido.getId() + " com valor: R$ " + valorTotal);
        System.out.println("✅ PEDIDO DEBUG - Pedido salvo com sucesso");
        
        // Cria os itens do pedido
        for (ItemCarrinho itemCarrinho : carrinho.getItens()) {
            // Recarrega o produto da sessão atual para evitar entidade desanexada
            Produto produto = produtoRepository.findById(itemCarrinho.getProduto().getId())
                .orElseThrow(() -> new RuntimeException("Produto não encontrado: " + itemCarrinho.getProduto().getId()));
            
            PedidoItem pedidoItem = new PedidoItem();
            // ID será gerado automaticamente
            pedidoItem.setProduto(produto);
            pedidoItem.setProdutoId(produto.getId());
            pedidoItem.setNome(produto.getNome());
            pedidoItem.setQuantidade(itemCarrinho.getQuantidade());
            pedidoItem.setPrecoUnitario(produto.getPreco());
            pedidoItem.setSubtotal(produto.getPreco()
                    .multiply(BigDecimal.valueOf(itemCarrinho.getQuantidade())));
            
            // Usa o método addItem para manter a relação bidirecional
            savedPedido.addItem(pedidoItem);
        }
        // Salva novamente para persistir os itens em cascata
        pedidoRepository.save(savedPedido);
        System.out.println("✅ PEDIDO DEBUG - " + carrinho.getItens().size() + " itens do pedido salvos");
        
        // Limpa o carrinho após criar o pedido
        carrinhoService.clearCarrinho(clienteId);
        System.out.println("✅ PEDIDO DEBUG - Carrinho foi limpo");
        
        // Registra informações do pedido
        System.out.println("📊 PEDIDO INFO - ID: " + savedPedido.getId());
        System.out.println("📊 PEDIDO INFO - Cliente ID: " + cliente.getId());
        System.out.println("📊 PEDIDO INFO - Total: R$ " + valorTotal);
        System.out.println("📊 PEDIDO INFO - Quantidade de itens: " + carrinho.getItens().size());
        
        // Envia notificação de confirmação ao cliente
        notificacaoService.criarNotificacaoConfirmacao(cliente, savedPedido);
        System.out.println("✅ PEDIDO DEBUG - Notificação enviada");
        
        PedidoResponseDTO response = pedidoMapper.toResponseDTO(savedPedido);
        System.out.println("✅ PEDIDO DEBUG - DTO mapeado, retornando response");
        System.out.println("🔍 MAPPER DEBUG - ID do pedido original: " + savedPedido.getId());
        System.out.println("🔍 MAPPER DEBUG - ID da resposta após mapeamento: " + response.getId());
        System.out.println("🔍 MAPPER DEBUG - IDs são iguais? " + savedPedido.getId().equals(response.getId()));
        return response;
    }
    
    /**
     * Busca pedido por ID
     * 
     * @param id O ID do pedido
     * @return O pedido encontrado ou vazio se não existir
     */
    public Optional<PedidoResponseDTO> findById(UUID id) {
        return pedidoRepository.findByIdWithItens(id)
                .map(pedidoMapper::toResponseDTO);
    }
    
    /**
     * Lista pedidos do cliente
     * 
     * @param clienteId O ID do cliente
     * @return Lista de pedidos do cliente
     */
    public List<PedidoResponseDTO> findByCliente(UUID clienteId) {
        return pedidoRepository.findByClienteId(clienteId).stream()
                .map(pedidoMapper::toResponseDTO)
                .toList();
    }
    
    /**
     * Lista pedidos por status
     * 
     * @param status O status dos pedidos a buscar
     * @return Lista de pedidos com o status especificado
     */
    public List<PedidoResponseDTO> findByStatus(StatusPedido status) {
        return pedidoRepository.findByStatus(status).stream()
                .map(pedidoMapper::toResponseDTO)
                .toList();
    }
    
    /**
     * Lista pedidos entre datas
     * 
     * @param inicio Data/hora inicial do período
     * @param fim Data/hora final do período
     * @return Lista de pedidos no período especificado
     */
    public List<PedidoResponseDTO> findByPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return pedidoRepository.findByDataPedidoBetween(inicio, fim).stream()
                .map(pedidoMapper::toResponseDTO)
                .toList();
    }
    
    /**
     * Atualiza o status do pedido
     * 
     * @param pedidoId O ID do pedido
     * @param novoStatus O novo status do pedido
     * @return O pedido atualizado
     * @throws RuntimeException se a transição de status não for permitida
     */
    @Transactional
    public PedidoResponseDTO atualizarStatus(UUID pedidoId, StatusPedido novoStatus) {
        Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoId);
        if (pedidoOpt.isEmpty()) {
            throw new RuntimeException("Pedido não encontrado");
        }
        
        Pedido pedido = pedidoOpt.get();
        StatusPedido statusAnterior = pedido.getStatus();
        
        if (!isTransicaoValida(statusAnterior, novoStatus)) {
            throw new RuntimeException("Transição de " + statusAnterior + " para " + novoStatus + " não é permitida");
        }
        
        pedido.setStatus(novoStatus);
        pedido.setUpdatedAt(Instant.now());
        
        if (novoStatus == StatusPedido.PAGO && pedido.getPaidAt() == null) {
            pedido.setPaidAt(Instant.now());
        }
        if (novoStatus == StatusPedido.CANCELADO && pedido.getCanceledAt() == null) {
            pedido.setCanceledAt(Instant.now());
        }
        
        Pedido savedPedido = pedidoRepository.save(pedido);
        
        notificacaoService.criarNotificacaoStatus(pedido.getCliente(), savedPedido, novoStatus);
        
        return pedidoMapper.toResponseDTO(savedPedido);
    }
    
    /**
     * Valida se a transição de status é permitida
     * 
     * @param from Status atual
     * @param to Novo status
     * @return true se a transição é válida, false caso contrário
     */
    private boolean isTransicaoValida(StatusPedido from, StatusPedido to) {
        return switch (from) {
            case NOVO -> to == StatusPedido.PROCESSANDO || to == StatusPedido.CANCELADO;
            case PROCESSANDO -> to == StatusPedido.PAGO || to == StatusPedido.CANCELADO;
            case PAGO -> to == StatusPedido.ENVIADO;
            case ENVIADO -> to == StatusPedido.ENTREGUE;
            case ENTREGUE -> false;
            case CANCELADO -> false;
        };
    }
    
    /**
     * Cancela pedido
     */
    public PedidoResponseDTO cancelarPedido(UUID clienteId, UUID pedidoId) {
        Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoId);
        if (pedidoOpt.isEmpty()) {
            throw new RuntimeException("Pedido não encontrado");
        }
        
        Pedido pedido = pedidoOpt.get();
        
        if (!pedido.getCliente().getId().equals(clienteId)) {
            throw new RuntimeException("Pedido não pertence ao cliente");
        }
        
        if (pedido.getStatus() == StatusPedido.PAGO) {
            throw new RuntimeException("Não é possível cancelar pedido já pago");
        }
        
        if (pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new RuntimeException("Pedido já está cancelado");
        }
        
        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setCanceledAt(Instant.now());
        pedido.setUpdatedAt(Instant.now());
        Pedido savedPedido = pedidoRepository.save(pedido);
        
        notificacaoService.criarNotificacaoStatus(pedido.getCliente(), savedPedido, StatusPedido.CANCELADO);
        
        return pedidoMapper.toResponseDTO(savedPedido);
    }
    
    /**
     * Lista todos os pedidos (para administradores)
     */
    public List<PedidoResponseDTO> findAll() {
        return pedidoRepository.findAll().stream()
                .map(pedidoMapper::toResponseDTO)
                .toList();
    }
    
    /**
     * Conta pedidos por status
     */
    public long countByStatus(StatusPedido status) {
        return pedidoRepository.countByStatus(status);
    }
    
    /**
     * Conta pedidos por cliente
     */
    public long countByCliente(UUID clienteId) {
        return pedidoRepository.countByCliente(clienteId);
    }
    
    /**
     * Calcula valor total do carrinho
     */
    private BigDecimal calcularValorTotal(Carrinho carrinho) {
        return carrinho.getItens().stream()
                .map(item -> item.getProduto().getPreco().multiply(BigDecimal.valueOf(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Confirma pedido com DTO completo (checkout completo) - Nova estrutura de payload
     */
    @Transactional
    public PedidoResponseDTO confirmarPedido(UUID clienteId, ConfirmarPedidoRequestDTO request) {
        System.out.println("🔄 PEDIDO - Confirmando pedido com nova estrutura de payload");
        
        // Validações de entrada
        PagamentoConfirmacaoDTO pagamento = request.getPagamento();
        
        if ("CARTAO".equals(pagamento.getMetodo())) {
            if (pagamento.getTokenCartao() == null || pagamento.getTokenCartao().trim().isEmpty()) {
                throw new IllegalArgumentException("Token do cartão é obrigatório para método CARTAO");
            }
            if (pagamento.getBandeira() == null || pagamento.getBandeira().trim().isEmpty()) {
                throw new IllegalArgumentException("Bandeira é obrigatória para método CARTAO");
            }
        }
        
        // Verifica idempotência
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Optional<Pedido> existingPedido = pedidoRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existingPedido.isPresent()) {
                System.out.println("✨ PEDIDO - Chave de idempotência encontrada, retornando pedido existente");
                return pedidoMapper.toResponseDTO(existingPedido.get());
            }
        }
        
        // Resolve cliente
        Cliente cliente = resolveCliente(clienteId);
        System.out.println("✅ PEDIDO - Cliente resolvido: " + cliente.getId());
        
        // Valida se o endereço pertence ao cliente autenticado
        Optional<Endereco> enderecoOpt = enderecoRepository.findByIdAndClienteId(request.getEnderecoId(), cliente.getId());
        if (enderecoOpt.isEmpty()) {
            throw new RuntimeException("Endereço não encontrado para este cliente");
        }
        
        Endereco endereco = enderecoOpt.get();
        
        // Busca carrinho
        Optional<Carrinho> carrinhoOpt = carrinhoRepository.findByClienteIdWithItens(cliente.getId());
        if (carrinhoOpt.isEmpty() || carrinhoOpt.get().getItens().isEmpty()) {
            throw new RuntimeException("Carrinho vazio ou não encontrado");
        }
        
        Carrinho carrinho = carrinhoOpt.get();
        BigDecimal valorTotal = calcularValorTotal(carrinho);
        
        // Cria pedido
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setClienteId(cliente.getId());
        pedido.setEnderecoEntrega(endereco);
        pedido.setDataPedido(LocalDateTime.now());
        pedido.setValorTotal(valorTotal);
        pedido.setTotal(valorTotal);
        pedido.setIdempotencyKey(request.getIdempotencyKey());
        
        // Gera número único do pedido no formato: PED-YYYYMMDD-XXXX
        String numeroPedido = gerarNumeroPedido();
        pedido.setNumero(numeroPedido);
        
        // Define prazo estimado de entrega (7 dias)
        pedido.setEtaEntrega(Instant.now().plusSeconds(7 * 24 * 60 * 60));
        
        // Define método de pagamento e status baseado no tipo de pagamento
        pedido.setMetodoPagamento(MetodoPagamento.valueOf(pagamento.getMetodo()));
        
        if ("CARTAO".equals(pagamento.getMetodo())) {
            pedido.setStatus(StatusPedido.PAGO); // Cartão aprovado = pedido pago
            pedido.setPaidAt(Instant.now());
            pedido.setTransactionId(pagamento.getTokenCartao());
            System.out.println("💳 PEDIDO - Pagamento com cartão aprovado, pedido marcado como PAGO");
        } else if ("BOLETO".equals(pagamento.getMetodo())) {
            pedido.setStatus(StatusPedido.PROCESSANDO); // Boleto = aguardando pagamento
            System.out.println("🧾 PEDIDO - Pagamento com boleto, pedido marcado como PROCESSANDO");
        }
        
        // Salva pedido
        Pedido savedPedido = pedidoRepository.save(pedido);
        System.out.println("✅ PEDIDO - Pedido salvo com ID: " + savedPedido.getId());
        
        // Cria os itens do pedido
        for (ItemCarrinho itemCarrinho : carrinho.getItens()) {
            // Recarrega o produto da sessão atual para evitar entidade desanexada
            Produto produto = produtoRepository.findById(itemCarrinho.getProduto().getId())
                .orElseThrow(() -> new RuntimeException("Produto não encontrado: " + itemCarrinho.getProduto().getId()));
            
            PedidoItem pedidoItem = new PedidoItem();
            pedidoItem.setProduto(produto);
            pedidoItem.setProdutoId(produto.getId());
            pedidoItem.setNome(produto.getNome());
            pedidoItem.setQuantidade(itemCarrinho.getQuantidade());
            pedidoItem.setPrecoUnitario(produto.getPreco());
            pedidoItem.setSubtotal(produto.getPreco().multiply(BigDecimal.valueOf(itemCarrinho.getQuantidade())));
            
            // Usa o método addItem para manter a relação bidirecional
            savedPedido.addItem(pedidoItem);
        }
        // Salva novamente para persistir os itens em cascata
        pedidoRepository.save(savedPedido);
        
        // Limpa o carrinho - passa o User ID original, não o Cliente ID
        carrinhoService.clearCarrinho(clienteId);
        System.out.println("🧹 PEDIDO - Carrinho limpo");
        
        // Envia notificação
        notificacaoService.criarNotificacaoConfirmacao(cliente, savedPedido);
        System.out.println("✅ PEDIDO - Notificação enviada");
        
        return pedidoMapper.toResponseDTO(savedPedido);
    }
    
    /**
     * Confirma pedido com simulação de pagamento
     */
    @Transactional
    public PedidoResponseDTO confirmarPedido(UUID clienteId, UUID enderecoId, SimulacaoPagamentoResponseDTO simulacao, String idempotencyKey) {
        System.out.println("🔄 PEDIDO - Confirmando pedido com simulação de pagamento");
        
        // Verifica idempotência
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Pedido> existingPedido = pedidoRepository.findByIdempotencyKey(idempotencyKey);
            if (existingPedido.isPresent()) {
                System.out.println("✨ PEDIDO - Chave de idempotência encontrada, retornando pedido existente");
                return pedidoMapper.toResponseDTO(existingPedido.get());
            }
        }
        
        // Resolve cliente
        Cliente cliente = resolveCliente(clienteId);
        System.out.println("✅ PEDIDO - Cliente resolvido: " + cliente.getId());
        
        // Valida se o endereço pertence ao cliente autenticado
        Optional<Endereco> enderecoOpt = enderecoRepository.findByIdAndClienteId(enderecoId, cliente.getId());
        if (enderecoOpt.isEmpty()) {
            throw new RuntimeException("Endereço não encontrado para este cliente");
        }
        
        Endereco endereco = enderecoOpt.get();
        
        // Busca carrinho
        Optional<Carrinho> carrinhoOpt = carrinhoRepository.findByClienteIdWithItens(cliente.getId());
        if (carrinhoOpt.isEmpty() || carrinhoOpt.get().getItens().isEmpty()) {
            throw new RuntimeException("Carrinho vazio ou não encontrado");
        }
        
        Carrinho carrinho = carrinhoOpt.get();
        BigDecimal valorTotal = calcularValorTotal(carrinho);
        
        // Valida valor com simulação
        if (simulacao.getValor().compareTo(valorTotal) != 0) {
            throw new RuntimeException("Valor da simulação não confere com o valor do carrinho");
        }
        
        // Cria pedido
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setClienteId(cliente.getId());
        pedido.setEnderecoEntrega(endereco);
        pedido.setDataPedido(LocalDateTime.now());
        pedido.setValorTotal(valorTotal);
        pedido.setTotal(valorTotal);
        
        // Gera número único do pedido no formato: PED-YYYYMMDD-XXXX
        String numeroPedido = gerarNumeroPedido();
        pedido.setNumero(numeroPedido);
        
        // Define prazo estimado de entrega (7 dias)
        pedido.setEtaEntrega(Instant.now().plusSeconds(7 * 24 * 60 * 60));
        
        // Define status baseado na simulação de pagamento
        if (simulacao.getStatus() == StatusPagamento.APROVADO) {
            pedido.setStatus(StatusPedido.PAGO);
            pedido.setPaidAt(Instant.now());
        } else if (simulacao.getStatus() == StatusPagamento.PENDENTE) {
            pedido.setStatus(StatusPedido.PROCESSANDO);
        } else {
            throw new RuntimeException("Pagamento recusado: " + simulacao.getMensagem());
        }
        
        // Salva metadata do pagamento
        pedido.setMetodoPagamento(simulacao.getMetodo());
        pedido.setTransactionId(simulacao.getTransacaoId());
        pedido.setPaymentStatus(simulacao.getStatus().toString());
        
        // Define idempotency key se fornecida
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            pedido.setIdempotencyKey(idempotencyKey);
        }
        
        // Salva pedido
        Pedido savedPedido = pedidoRepository.save(pedido);
        System.out.println("💾 PEDIDO - Pedido salvo: " + savedPedido.getNumero() + " com status: " + savedPedido.getStatus());
        
        // Cria os itens do pedido
        for (ItemCarrinho itemCarrinho : carrinho.getItens()) {
            // Recarrega o produto da sessão atual para evitar entidade desanexada
            Produto produto = produtoRepository.findById(itemCarrinho.getProduto().getId())
                .orElseThrow(() -> new RuntimeException("Produto não encontrado: " + itemCarrinho.getProduto().getId()));
            
            PedidoItem pedidoItem = new PedidoItem();
            pedidoItem.setProduto(produto);
            pedidoItem.setProdutoId(produto.getId());
            pedidoItem.setNome(produto.getNome());
            pedidoItem.setQuantidade(itemCarrinho.getQuantidade());
            pedidoItem.setPrecoUnitario(produto.getPreco());
            pedidoItem.setSubtotal(produto.getPreco()
                    .multiply(BigDecimal.valueOf(itemCarrinho.getQuantidade())));
            
            // Usa o método addItem para manter a relação bidirecional
            savedPedido.addItem(pedidoItem);
        }
        // Salva novamente para persistir os itens em cascata
        pedidoRepository.save(savedPedido);
        System.out.println("✅ PEDIDO - " + carrinho.getItens().size() + " itens salvos");
        
        // Limpa carrinho
        carrinhoService.clearCarrinho(clienteId);
        System.out.println("🧑 PEDIDO - Carrinho limpo");
        
        // Envia notificação
        notificacaoService.criarNotificacaoConfirmacao(cliente, savedPedido);
        
        // Retorna pedido completo
        PedidoResponseDTO response = pedidoMapper.toResponseDTO(savedPedido);
        System.out.println("✅ PEDIDO - Pedido confirmado com sucesso: " + savedPedido.getNumero());
        return response;
    }
    
    /**
     * Gera número único para o pedido no formato PED-YYYYMMDD-XXXX
     */
    private String gerarNumeroPedido() {
        LocalDateTime agora = LocalDateTime.now();
        String data = agora.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int sequencia = sequenciaNumero.getAndIncrement();
        
        // Se passou para o próximo dia, reinicia a sequência
        if (sequencia > 9999) {
            sequenciaNumero.set(1);
            sequencia = sequenciaNumero.getAndIncrement();
        }
        
        return String.format("PED-%s-%04d", data, sequencia);
    }
    
    
    /**
     * Verifica se existe pedido com a idempotency key fornecida
     */
    public boolean existePedidoComIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return false;
        }
        return pedidoRepository.findByIdempotencyKey(idempotencyKey).isPresent();
    }
    
    /**
     * Lista pedidos do cliente - CORRIGIDO para resolver Cliente a partir do userId
     */
    public List<PedidoResponseDTO> getMeusPedidos(UUID userId) {
        System.out.println("📋 MEUS PEDIDOS - Buscando pedidos para userId: " + userId);
        
        // Resolver Cliente primeiro (User ID → Cliente)
        Cliente cliente = resolveCliente(userId);
        System.out.println("📋 MEUS PEDIDOS - Cliente resolvido com ID: " + cliente.getId());
        
        // Buscar pedidos usando o ID real do Cliente, não o userId
        List<Pedido> pedidos = pedidoRepository.findByClienteIdOrderByCreatedAtDesc(cliente.getId());
        System.out.println("📋 MEUS PEDIDOS - Encontrados " + pedidos.size() + " pedidos");
        
        return pedidos.stream()
                .map(pedidoMapper::toResponseDTO)
                .toList();
    }
    
    public List<PedidoResponseDTO> getPedidosAdmin() {
        return pedidoRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(pedidoMapper::toResponseDTO)
                .toList();
    }
    
    public long countPedidos() {
        return pedidoRepository.count();
    }
    
    public long countClientes() {
        return pedidoRepository.countDistinctClienteId();
    }
    
    public BigDecimal getTotalFaturamento() {
        return pedidoRepository.findByStatus(StatusPedido.PAGO).stream()
                .map(p -> p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public long countClientesPagos() {
        return pedidoRepository.findByStatus(StatusPedido.PAGO).stream()
                .map(Pedido::getClienteId)
                .distinct()
                .count();
    }
    
    @Transactional
    public PedidoResponseDTO finalizarCarrinho(UUID userId, String idempotencyKey) {
        System.out.println("🛒 CHECKOUT - Iniciando finalização do carrinho para userId: " + userId);
        
        // Verificar idempotência
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Pedido> existingPedido = pedidoRepository.findByIdempotencyKey(idempotencyKey);
            if (existingPedido.isPresent()) {
                System.out.println("✅ CHECKOUT - Idempotency key encontrada, retornando pedido existente");
                return pedidoMapper.toResponseDTO(existingPedido.get());
            }
        }
        
        // Resolver cliente
        Cliente cliente = resolveCliente(userId);
        System.out.println("✅ CHECKOUT - Cliente resolvido: " + cliente.getId());
        
        // Carregar carrinho
        Optional<Carrinho> carrinhoOpt = carrinhoRepository.findByClienteIdWithItens(cliente.getId());
        if (carrinhoOpt.isEmpty() || carrinhoOpt.get().getItens().isEmpty()) {
            throw new RuntimeException("Carrinho vazio ou não encontrado");
        }
        
        Carrinho carrinho = carrinhoOpt.get();
        System.out.println("✅ CHECKOUT - Carrinho encontrado com " + carrinho.getItens().size() + " itens");
        
        // Criar pedido
        Pedido pedido = new Pedido();
        pedido.setClienteId(cliente.getId());
        pedido.setCliente(cliente);
        pedido.setStatus(StatusPedido.NOVO);
        pedido.setIdempotencyKey(idempotencyKey);
        
        BigDecimal total = BigDecimal.ZERO;
        
        // Criar itens do pedido com snapshot dos dados
        for (ItemCarrinho itemCarrinho : carrinho.getItens()) {
            PedidoItem pedidoItem = new PedidoItem();
            pedidoItem.setProdutoId(itemCarrinho.getProduto().getId());
            pedidoItem.setNome(itemCarrinho.getProduto().getNome());
            pedidoItem.setPrecoUnitario(itemCarrinho.getProduto().getPreco());
            pedidoItem.setQuantidade(itemCarrinho.getQuantidade());
            pedidoItem.setSubtotal(itemCarrinho.getProduto().getPreco()
                    .multiply(BigDecimal.valueOf(itemCarrinho.getQuantidade())));
            
            // Usar método auxiliar que configura relacionamento bidirecional
            pedido.addItem(pedidoItem);
            
            total = total.add(pedidoItem.getSubtotal());
        }
        
        pedido.setTotal(total);
        pedido.setValorTotal(total);
        
        // Persistir pedido com itens (cascade deve cuidar dos itens)
        Pedido savedPedido = pedidoRepository.save(pedido);
        System.out.println("✅ CHECKOUT - Pedido criado: " + savedPedido.getId() + " com total: R$ " + total);
        
        // Limpar carrinho
        carrinhoService.clearCarrinho(userId);
        System.out.println("✅ CHECKOUT - Carrinho limpo");
        
        // Enviar notificação
        notificacaoService.criarNotificacaoConfirmacao(cliente, savedPedido);
        
        return pedidoMapper.toResponseDTO(savedPedido);
    }
    
    @Transactional
    public PedidoResponseDTO criarPedido(PedidoRequestDTO requestDTO) {
        // Se origem for carrinho, usar finalizarCarrinho
        if ("carrinho".equals(requestDTO.getOrigem())) {
            return finalizarCarrinho(requestDTO.getClienteId(), requestDTO.getIdempotencyKey());
        }
        
        // Caso contrário, usar lógica existente
        if (requestDTO.getIdempotencyKey() != null && !requestDTO.getIdempotencyKey().isBlank()) {
            Optional<Pedido> existingPedido = pedidoRepository.findByIdempotencyKey(requestDTO.getIdempotencyKey());
            if (existingPedido.isPresent()) {
                return pedidoMapper.toResponseDTO(existingPedido.get());
            }
        }
        
        Cliente cliente = resolveCliente(requestDTO.getClienteId());
        
        Pedido pedido = new Pedido();
        pedido.setClienteId(cliente.getId());
        pedido.setCliente(cliente);
        pedido.setStatus(StatusPedido.NOVO);
        pedido.setIdempotencyKey(requestDTO.getIdempotencyKey());
        
        BigDecimal total = BigDecimal.ZERO;
        if (requestDTO.getItens() != null && !requestDTO.getItens().isEmpty()) {
            for (ItemPedidoDTO item : requestDTO.getItens()) {
                PedidoItem pedidoItem = new PedidoItem();
                pedidoItem.setProdutoId(item.getProdutoId());
                pedidoItem.setNome(item.getNome() != null ? item.getNome() : "Produto");
                pedidoItem.setQuantidade(item.getQuantidade());
                pedidoItem.setPrecoUnitario(item.getPrecoUnitario());
                pedidoItem.setSubtotal(item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())));
                
                // Usar método auxiliar que configura relacionamento bidirecional
                pedido.addItem(pedidoItem);
                
                total = total.add(pedidoItem.getSubtotal());
            }
        }
        
        pedido.setValorTotal(total);
        pedido.setTotal(total);
        
        if (requestDTO.getEnderecoId() != null) {
            Optional<Endereco> enderecoOpt = enderecoRepository.findById(requestDTO.getEnderecoId());
            pedido.setEnderecoEntrega(enderecoOpt.orElse(null));
        }
        
        // Persistir pedido com itens
        Pedido savedPedido = pedidoRepository.save(pedido);
        
        carrinhoService.clearCarrinho(requestDTO.getClienteId());
        
        notificacaoService.criarNotificacaoConfirmacao(cliente, savedPedido);
        
        return pedidoMapper.toResponseDTO(savedPedido);
    }
}