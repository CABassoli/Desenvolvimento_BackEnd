package com.ecommerce;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import io.javalin.http.staticfiles.Location;
import com.ecommerce.config.DatabaseConfig;
import com.ecommerce.config.TransactionFilter;
import com.ecommerce.controller.*;
import com.ecommerce.service.*;
import com.ecommerce.repository.*;
import com.ecommerce.mapper.*;
import com.ecommerce.mapper.CarrinhoMapperImpl;
import com.ecommerce.mapper.CategoriaMapperImpl;
import com.ecommerce.mapper.ClienteMapperImpl;
import com.ecommerce.mapper.EnderecoMapperImpl;
import com.ecommerce.mapper.ItemCarrinhoMapperImpl;
import com.ecommerce.mapper.ItemPedidoMapperImpl;
import com.ecommerce.mapper.NotificacaoMapperImpl;
import com.ecommerce.mapper.PagamentoMapperImpl;
import com.ecommerce.mapper.PedidoMapperImpl;
import com.ecommerce.mapper.ProdutoMapperImpl;
import com.ecommerce.mapper.UserMapperImpl;
import com.ecommerce.security.OwnershipValidator;
import jakarta.persistence.EntityManager;
import java.util.Map;

public class App {
    
    private static final int PORT = 5000;
    private static JwtService jwtService;
    
    /**
     * Cria e configura uma aplicação Javalin para propósitos de teste
     */
    public static Javalin createApp() {
        // Inicializa o banco de dados
        DatabaseConfig.initialize();
        EntityManager entityManager = DatabaseConfig.createEntityManager();
        
        // Inicializa serviços e controladores
        initializeServicesAndControllers(entityManager);
        
        // Cria a aplicação Javalin
        Javalin app = Javalin.create(config -> {
            // Vincula a todas as interfaces
            config.jetty.defaultHost = "0.0.0.0";
            
            // Serve arquivos estáticos de /static no classpath
            config.staticFiles.add(s -> { 
                s.hostedPath = "/"; 
                s.directory = "/static"; 
                s.location = Location.CLASSPATH;
                // Desabilita cache para desenvolvimento
                s.headers = Map.of(
                    "Cache-Control", "no-cache, no-store, must-revalidate",
                    "Pragma", "no-cache",
                    "Expires", "0"
                );
            });
            
            // Habilita CORS para desenvolvimento
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(corsRule -> {
                    corsRule.anyHost();
                    corsRule.allowCredentials = true;
                });
            });
            
            // Habilita log de requisições
            config.bundledPlugins.enableDevLogging();
        });
        
        // Configura as rotas
        configureRoutes(app);
        
        return app;
    }
    
    public static void main(String[] args) {
        System.out.println("🚀 E-commerce API iniciando...");
        System.out.println("📅 " + java.time.LocalDateTime.now());
        
        // Inicializa o banco de dados
        System.out.println("🗄️ Inicializando banco de dados...");
        long startTime = System.currentTimeMillis();
        DatabaseConfig.initialize();
        System.out.println("✅ Banco inicializado em " + (System.currentTimeMillis() - startTime) + "ms");
        
        System.out.println("🔌 Criando EntityManager inicial...");
        EntityManager entityManager = DatabaseConfig.createEntityManager();
        System.out.println("✅ EntityManager criado");
        
        // Inicializa serviços e controladores
        System.out.println("⚙️ Inicializando serviços e controladores...");
        startTime = System.currentTimeMillis();
        initializeServicesAndControllers(entityManager);
        System.out.println("✅ Serviços inicializados em " + (System.currentTimeMillis() - startTime) + "ms");
        
        // Cria a aplicação Javalin
        Javalin app = Javalin.create(config -> {
            // Vincula a todas as interfaces
            config.jetty.defaultHost = "0.0.0.0";
            
            // Serve arquivos estáticos de /static no classpath
            config.staticFiles.add(s -> { 
                s.hostedPath = "/"; 
                s.directory = "/static"; 
                s.location = Location.CLASSPATH;
                // Desabilita cache para desenvolvimento
                s.headers = Map.of(
                    "Cache-Control", "no-cache, no-store, must-revalidate",
                    "Pragma", "no-cache",
                    "Expires", "0"
                );
            });
            
            // Habilita CORS para desenvolvimento
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(corsRule -> {
                    corsRule.anyHost();
                    corsRule.allowCredentials = true;
                });
            });
            
            // Habilita log de requisições
            config.bundledPlugins.enableDevLogging();
        });
        
        // Configura as rotas
        configureRoutes(app);
        
        // Inicia servidor em todas as interfaces para compatibilidade com workflow
        app.start("0.0.0.0", PORT);
        
        // Hook de desligamento
        Runtime.getRuntime().addShutdownHook(new Thread(DatabaseConfig::shutdown));
        
        System.out.println("✅ E-commerce API iniciada na porta " + PORT);
        System.out.println("🌐 Acesse: http://localhost:" + PORT);
        System.out.println("❤️ Health Check: http://localhost:" + PORT + "/health");
        System.out.println("📊 H2 Console: http://localhost:" + PORT + "/h2-console");
        System.out.println("📚 Documentação da API:");
        System.out.println("   📋 Autenticação: POST /auth/login | POST /auth/register");
        System.out.println("   📦 Produtos: GET|POST /produtos | GET /categorias");
        System.out.println("   👥 Clientes: GET|POST /clientes | GET|POST /clientes/{id}/enderecos");
        System.out.println("   🛒 Carrinho: GET|POST /carrinho/{clienteId} | POST /carrinho/{clienteId}/itens");
        System.out.println("   📄 Pedidos: POST /pedidos/{clienteId} | GET /pedidos/cliente/{clienteId}");
        System.out.println("   💳 Pagamentos: POST /pagamentos/{pedidoId}/pix|cartao|boleto");
        System.out.println("   🔔 Notificações: GET /notificacoes/cliente/{clienteId}");
    }
    
    // Controladores - serão inicializados em initializeServicesAndControllers
    private static AuthController authController;
    private static CategoriaController categoriaController;
    private static ProdutoController produtoController;
    private static ClienteController clienteController;
    private static EnderecoController enderecoController;
    private static CarrinhoController carrinhoController;
    private static PedidoController pedidoController;
    private static PagamentoController pagamentoController;
    private static NotificacaoController notificacaoController;
    private static AdminController adminController;
    private static OwnershipValidator ownershipValidator;
    
    private static void initializeServicesAndControllers(EntityManager entityManager) {
        System.out.println("📊 Inicializando mapeadores...");
        // Inicializa mapeadores
        UserMapper userMapper = new UserMapperImpl();
        CategoriaMapper categoriaMapper = new CategoriaMapperImpl();
        ProdutoMapper produtoMapper = new ProdutoMapperImpl();
        ClienteMapper clienteMapper = new ClienteMapperImpl();
        EnderecoMapper enderecoMapper = new EnderecoMapperImpl();
        CarrinhoMapper carrinhoMapper = new CarrinhoMapperImpl();
        ItemCarrinhoMapper itemCarrinhoMapper = new ItemCarrinhoMapperImpl();
        PedidoMapper pedidoMapper = new PedidoMapperImpl();
        PagamentoMapper pagamentoMapper = new PagamentoMapperImpl();
        NotificacaoMapper notificacaoMapper = new NotificacaoMapperImpl();
        System.out.println("✅ Mapeadores inicializados");
        
        System.out.println("📦 Inicializando repositórios...");
        // Inicializa repositórios
        UserRepository userRepository = new UserRepository(entityManager);
        CategoriaRepository categoriaRepository = new CategoriaRepository(entityManager);
        ProdutoRepository produtoRepository = new ProdutoRepository(entityManager);
        ClienteRepository clienteRepository = new ClienteRepository(entityManager);
        EnderecoRepository enderecoRepository = new EnderecoRepository(entityManager);
        CarrinhoRepository carrinhoRepository = new CarrinhoRepository(entityManager);
        ItemCarrinhoRepository itemCarrinhoRepository = new ItemCarrinhoRepository(entityManager);
        PedidoRepository pedidoRepository = new PedidoRepository(entityManager);
        ItemPedidoRepository itemPedidoRepository = new ItemPedidoRepository(entityManager);
        PagamentoRepository pagamentoRepository = new PagamentoRepository(entityManager);
        NotificacaoRepository notificacaoRepository = new NotificacaoRepository(entityManager);
        System.out.println("✅ Repositórios inicializados");
        
        System.out.println("🔧 Inicializando serviços...");
        // Initialize services
        System.out.println("  🔐 Criando JwtService...");
        jwtService = new JwtService();
        System.out.println("  👤 Criando UserService...");
        UserService userService = new UserService(userRepository, userMapper, jwtService, clienteRepository);
        System.out.println("  📂 Criando CategoriaService...");
        CategoriaService categoriaService = new CategoriaService(categoriaRepository, produtoRepository, categoriaMapper);
        System.out.println("  📦 Criando ProdutoService...");
        ProdutoService produtoService = new ProdutoService(produtoRepository, categoriaRepository, produtoMapper);
        System.out.println("  👥 Criando ClienteService...");
        ClienteService clienteService = new ClienteService(clienteRepository, clienteMapper);
        System.out.println("  🏠 Criando EnderecoService...");
        EnderecoService enderecoService = new EnderecoService(enderecoRepository, clienteRepository, userRepository, pedidoRepository, enderecoMapper);
        System.out.println("  🛒 Criando CarrinhoService...");
        CarrinhoService carrinhoService = new CarrinhoService(carrinhoRepository, itemCarrinhoRepository, clienteRepository, produtoRepository, userRepository, carrinhoMapper, itemCarrinhoMapper);
        System.out.println("  🔔 Criando NotificacaoService...");
        NotificacaoService notificacaoService = new NotificacaoService(notificacaoRepository, notificacaoMapper);
        System.out.println("  📄 Criando PedidoService...");
        PedidoItemRepository pedidoItemRepository = new PedidoItemRepository(entityManager);
        PedidoService pedidoService = new PedidoService(pedidoRepository, clienteRepository, enderecoRepository, carrinhoRepository, itemCarrinhoRepository, itemPedidoRepository, pedidoItemRepository, userRepository, pedidoMapper, notificacaoService, carrinhoService, produtoRepository);
        System.out.println("  💳 Criando PagamentoService...");
        PagamentoService pagamentoService = new PagamentoService(pagamentoRepository, pedidoRepository, pagamentoMapper, notificacaoService);
        
        // Inicializa validador de segurança
        ownershipValidator = new OwnershipValidator(pedidoRepository, clienteRepository, enderecoRepository, carrinhoRepository);
        
        // Inicializa controladores
        authController = new AuthController(userService);
        categoriaController = new CategoriaController(categoriaService);
        produtoController = new ProdutoController(produtoService);
        clienteController = new ClienteController(clienteService);
        enderecoController = new EnderecoController(enderecoService);
        carrinhoController = new CarrinhoController(carrinhoService);
        pedidoController = new PedidoController(pedidoService);
        pagamentoController = new PagamentoController(pagamentoService);
        notificacaoController = new NotificacaoController(notificacaoService);
        adminController = new AdminController(pedidoService, produtoService, clienteService);
    }
    
    private static void configureRoutes(Javalin app) {
        // Registrar TransactionFilter para gerenciar transações por request
        // Before handler - cria EntityManager e inicia transação
        app.before("/*", new TransactionFilter.BeforeHandler());
        
        // After handler - faz commit/rollback e limpa recursos
        app.after("/*", new TransactionFilter.AfterHandler());
        
        // Exception handler - garante rollback em caso de exceção
        app.exception(Exception.class, new TransactionFilter.ExceptionHandler());
        
        // Endpoint de verificação de saúde
        app.get("/health", ctx -> {
            ctx.json(Map.of(
                "status", "OK",
                "message", "API E-commerce está rodando", 
                "port", PORT,
                "timestamp", java.time.Instant.now(),
                "version", "1.0.0"
            ));
        });
        
        // Endpoints do console H2 (apenas em desenvolvimento)
        String environment = System.getProperty("environment", "development");
        if ("development".equals(environment)) {
            app.get("/h2-console", ctx -> ctx.redirect("http://localhost:" + PORT + "/h2"));
            app.get("/h2", ctx -> {
                String html = "<!DOCTYPE html><html><head><title>H2 Console</title></head>" +
                        "<body><h2>📊 H2 Database Console</h2>" +
                        "<p><strong>JDBC URL:</strong> jdbc:h2:mem:ecommerce</p>" +
                        "<p><strong>User:</strong> sa</p>" +
                        "<p><strong>Password:</strong> (deixe vazio)</p>" +
                        "<p>Para conectar ao banco, use um cliente H2 externo com as credenciais acima.</p>" +
                        "<a href='/'>← Voltar para API</a></body></html>";
                ctx.html(html);
            });
        }
        
        // Endpoint padrão com documentação da API
        
        // ==== ROTAS PÚBLICAS (Sem autenticação requerida) ====
        
        // Rotas de autenticação (API)
        app.post("/api/auth/login", authController::login);
        app.post("/api/auth/register", authController::register);
        
        // Rotas de autenticação legadas (para compatibilidade)
        app.post("/auth/login", authController::login);
        app.post("/auth/register", authController::register);
        
        // Rotas de categorias (leitura pública)
        app.get("/categorias", categoriaController::findAll);
        app.get("/categorias/{id}", categoriaController::findById);
        app.get("/categorias/nome/{nome}", categoriaController::findByNome);
        app.get("/categorias/count", categoriaController::count);
        
        // Rotas de produtos (leitura pública)
        app.get("/produtos", produtoController::findAll);
        app.get("/produtos/{id}", produtoController::findById);
        app.get("/produtos/codigo/{codigo}", produtoController::findByCodigoBarras);
        app.get("/produtos/categoria/{categoriaId}", produtoController::findByCategoria);
        app.get("/produtos/buscar/{nome}", produtoController::findByNome);
        app.get("/produtos/preco", produtoController::findByPrecoRange);
        
        // ==== ROTAS PROTEGIDAS (Requer autenticação) ====
        
        // Aplica middleware JWT em todas rotas /api/* exceto rotas públicas de autenticação
        app.before("/api/*", ctx -> {
            String path = ctx.path();
            // Pula autenticação apenas para rotas públicas
            if (!path.equals("/api/auth/login") && 
                !path.equals("/api/auth/register") && 
                !path.equals("/api/health")) {
                jwtMiddleware.handle(ctx);
            }
        });
        
        // Aplica middleware JWT em outras rotas protegidas
        app.before("/auth/profile", jwtMiddleware);
        app.before("/auth/change-password", jwtMiddleware);
        app.before("/clientes*", jwtMiddleware);
        app.before("/enderecos*", jwtMiddleware);
        app.before("/carrinho*", jwtMiddleware);
        app.before("/pedidos*", jwtMiddleware);
        app.before("/pagamentos*", jwtMiddleware);
        app.before("/notificacoes*", jwtMiddleware);
        
        // Aplica autorização JWT + MANAGER apenas nos endpoints admin
        app.before("/categorias", ctx -> {
            String method = ctx.method().name();
            if ("POST".equals(method)) {
                jwtMiddleware.handle(ctx);
                requireManager.handle(ctx);
            }
        });
        app.before("/categorias/*", ctx -> {
            String method = ctx.method().name();
            if ("PUT".equals(method) || "DELETE".equals(method)) {
                jwtMiddleware.handle(ctx);
                requireManager.handle(ctx);
            }
        });
        app.before("/produtos", ctx -> {
            String method = ctx.method().name();
            if ("POST".equals(method)) {
                jwtMiddleware.handle(ctx);
                requireManager.handle(ctx);
            }
        });
        app.before("/produtos/*", ctx -> {
            String method = ctx.method().name();
            if ("PUT".equals(method) || "DELETE".equals(method)) {
                jwtMiddleware.handle(ctx);
                requireManager.handle(ctx);
            }
        });
        
        // Segurança de pagamentos - MANAGER apenas para operações de leitura e confirmação de boleto
        app.before("/pagamentos", ctx -> {
            String method = ctx.method().name();
            if ("GET".equals(method)) {
                requireManager.handle(ctx);
            }
        });
        app.before("/pagamentos/{id}", ctx -> {
            String method = ctx.method().name();
            if ("GET".equals(method)) {
                requireManager.handle(ctx);
            }
        });
        app.before("/pagamentos/pix", ctx -> {
            String method = ctx.method().name();
            if ("GET".equals(method)) {
                requireManager.handle(ctx);
            }
        });
        app.before("/pagamentos/cartao", ctx -> {
            String method = ctx.method().name();
            if ("GET".equals(method)) {
                requireManager.handle(ctx);
            }
        });
        app.before("/pagamentos/boleto", ctx -> {
            String method = ctx.method().name();
            if ("GET".equals(method)) {
                requireManager.handle(ctx);
            }
        });
        app.before("/pagamentos/total", ctx -> {
            String method = ctx.method().name();
            if ("GET".equals(method)) {
                requireManager.handle(ctx);
            }
        });
        app.before("/pagamentos/count", ctx -> {
            String method = ctx.method().name();
            if ("GET".equals(method)) {
                requireManager.handle(ctx);
            }
        });
        app.before("/pagamentos/boleto/confirmar/*", ctx -> {
            String method = ctx.method().name();
            if ("PUT".equals(method)) {
                requireManager.handle(ctx);
            }
        });
        
        // Validação de propriedade - Controle de acesso aos dados do cliente
        // Validação de propriedade do pedido
        app.before("/pedidos/{id}", ownershipValidator::validatePedidoOwnership);
        app.before("/pedidos/{id}/*", ownershipValidator::validatePedidoOwnership);
        
        // Validação de propriedade do cliente
        app.before("/clientes/{clienteId}", ownershipValidator::validateClienteOwnership);
        app.before("/clientes/{clienteId}/*", ownershipValidator::validateClienteOwnership);
        app.before("/carrinho/{clienteId}", ownershipValidator::validateClienteOwnership);
        app.before("/carrinho/{clienteId}/*", ownershipValidator::validateClienteOwnership);
        app.before("/pedidos/cliente/{clienteId}", ownershipValidator::validateClienteOwnership);
        app.before("/pedidos/count/cliente/{clienteId}", ownershipValidator::validateClienteOwnership);
        app.before("/notificacoes/cliente/{clienteId}", ownershipValidator::validateClienteOwnership);
        app.before("/notificacoes/cliente/{clienteId}/*", ownershipValidator::validateClienteOwnership);
        app.before("/notificacoes/count/cliente/{clienteId}", ownershipValidator::validateClienteOwnership);
        
        // Address ownership validation
        app.before("/enderecos/{enderecoId}", ownershipValidator::validateEnderecoOwnership);
        app.before("/clientes/{clienteId}/enderecos/{enderecoId}", ownershipValidator::validateEnderecoOwnership);
        
        // Payment ownership validation - customers can only see their own payment data
        app.before("/pagamentos/pedido/{pedidoId}", ownershipValidator::validatePedidoOwnership);
        
        // Order status updates - MANAGER only for status changes
        app.before("/pedidos/{id}/status/*", ctx -> {
            requireManager.handle(ctx);
        });
        
        // Auth protected routes
        app.get("/auth/profile", authController::getProfile);
        app.put("/auth/change-password", authController::changePassword);
        app.get("/api/auth/profile", authController::getProfile);
        app.get("/api/auth/me", authController::getMe);
        app.put("/api/auth/change-password", authController::changePassword);
        
        // Category management (admin routes - MANAGER required)
        app.post("/categorias", categoriaController::create);
        app.put("/categorias/{id}", categoriaController::update);
        app.delete("/categorias/{id}", categoriaController::delete);
        
        // Product management (admin routes - MANAGER required)
        app.post("/produtos", produtoController::create);
        app.put("/produtos/{id}", produtoController::update);
        app.delete("/produtos/{id}", produtoController::delete);
        
        // Client routes
        app.get("/clientes", clienteController::findAll);
        app.post("/clientes", clienteController::create);
        app.get("/clientes/{id}", clienteController::findById);
        app.get("/clientes/{id}/enderecos", clienteController::findByIdWithEnderecos);
        app.get("/clientes/email/{email}", clienteController::findByEmail);
        app.get("/clientes/buscar/{nome}", clienteController::findByNome);
        app.put("/clientes/{id}", clienteController::update);
        app.delete("/clientes/{id}", clienteController::delete);
        app.get("/clientes/count", clienteController::count);
        
        // Address routes
        app.post("/clientes/{clienteId}/enderecos", enderecoController::create);
        app.get("/enderecos/{id}", enderecoController::findById);
        app.get("/clientes/{clienteId}/enderecos", enderecoController::findByCliente);
        app.get("/enderecos/cidade/{cidade}", enderecoController::findByCidade);
        app.get("/enderecos/cep/{cep}", enderecoController::findByCep);
        app.put("/clientes/{clienteId}/enderecos/{enderecoId}", enderecoController::update);
        app.delete("/clientes/{clienteId}/enderecos/{enderecoId}", enderecoController::delete);
        app.get("/clientes/{clienteId}/enderecos/count", enderecoController::countByCliente);
        
        // New Address API routes with JWT authentication
        app.before("/api/enderecos", jwtMiddleware);
        app.before("/api/enderecos/*", jwtMiddleware);
        
        app.get("/api/enderecos", enderecoController::listarEnderecos);
        app.get("/api/enderecos/{id}", enderecoController::buscarEnderecoPorId);
        app.post("/api/enderecos", enderecoController::criarEndereco);
        app.patch("/api/enderecos/{id}", enderecoController::atualizarEndereco);
        app.delete("/api/enderecos/{id}", enderecoController::removerEndereco);
        
        // Cart routes
        app.get("/carrinho/{clienteId}", carrinhoController::getCarrinho);
        app.post("/carrinho/{clienteId}/itens", carrinhoController::addItem);
        app.put("/carrinho/{clienteId}/itens/{itemId}", carrinhoController::updateItem);
        app.delete("/carrinho/{clienteId}/itens/{itemId}", carrinhoController::removeItem);
        app.delete("/carrinho/{clienteId}", carrinhoController::clearCarrinho);
        app.get("/carrinho/{clienteId}/itens", carrinhoController::getItens);
        app.get("/carrinho/{clienteId}/total", carrinhoController::calcularTotal);
        app.get("/carrinho/{clienteId}/count", carrinhoController::contarItens);
        // New Cart API routes with JWT authentication
        app.before("/api/carrinho", jwtMiddleware);
        app.before("/api/carrinho/*", jwtMiddleware);
        
        app.get("/api/carrinho", carrinhoController::getCarrinho);
        app.post("/api/carrinho/item", carrinhoController::addItem);
        app.delete("/api/carrinho/item/{produtoId}", carrinhoController::removeItem);
        app.delete("/api/carrinho", carrinhoController::clearCarrinho);
        
        // Order routes
        app.post("/pedidos/{clienteId}", pedidoController::confirmarPedidoLegacy);
        app.get("/pedidos", pedidoController::findAll);
        app.get("/pedidos/{id}", pedidoController::findById);
        app.get("/pedidos/cliente/{clienteId}", pedidoController::findByCliente);
        app.get("/pedidos/status/{status}", pedidoController::findByStatus);
        app.get("/pedidos/periodo", pedidoController::findByPeriodo);
        app.put("/pedidos/{id}/status/{status}", pedidoController::atualizarStatus);
        app.put("/pedidos/{id}/cancelar/{clienteId}", pedidoController::cancelarPedido);
        app.get("/pedidos/count/status/{status}", pedidoController::countByStatus);
        app.get("/pedidos/count/cliente/{clienteId}", pedidoController::countByCliente);
        // New Order API routes
        app.post("/api/pedidos", pedidoController::criarPedido);
        app.post("/api/pedidos/checkout", pedidoController::checkout);
        app.post("/api/pedidos/confirmar", pedidoController::confirmarPedido);  // New checkout confirmation endpoint
        app.get("/api/pedidos/me", pedidoController::getMeusPedidos);
        app.get("/api/admin/pedidos", pedidoController::getPedidosAdmin);
        // Admin metrics route
        app.get("/api/admin/metricas", adminController::getMetricas);
        // Admin update order status
        app.patch("/api/admin/pedidos/{id}/status", adminController::updateOrderStatus);
        // Client API route
        app.get("/api/clientes", clienteController::findAll);
        // Category API route
        app.get("/api/categorias", categoriaController::findAll);
        // Product API route  
        app.get("/api/produtos", produtoController::findAll);
        
        // Payment routes
        app.post("/pagamentos/{pedidoId}/pix", pagamentoController::processarPagamentoPix);
        app.post("/pagamentos/{pedidoId}/boleto", pagamentoController::processarPagamentoBoleto);
        app.put("/pagamentos/boleto/confirmar/{linhaDigitavel}", pagamentoController::confirmarPagamentoBoleto);
        app.get("/pagamentos", pagamentoController::findAll);
        app.get("/pagamentos/{id}", pagamentoController::findById);
        app.get("/pagamentos/pedido/{pedidoId}", pagamentoController::findByPedido);
        app.get("/pagamentos/pix", pagamentoController::findAllPix);
        app.get("/pagamentos/cartao", pagamentoController::findAllCartao);
        app.get("/pagamentos/boleto", pagamentoController::findAllBoleto);
        app.get("/pagamentos/total", pagamentoController::calcularTotal);
        app.get("/pagamentos/count", pagamentoController::count);
        
        // New Payment API routes with JWT authentication
        app.before("/api/pagamentos/simular", jwtMiddleware);
        app.post("/api/pagamentos/simular", pagamentoController::simularPagamento);  // Payment simulation endpoint
        
        // Notification routes
        app.get("/notificacoes", notificacaoController::findAll);
        app.get("/notificacoes/{id}", notificacaoController::findById);
        app.get("/notificacoes/cliente/{clienteId}", notificacaoController::findByCliente);
        app.get("/notificacoes/cliente/{clienteId}/recentes", notificacaoController::findRecentByCliente);
        app.get("/notificacoes/pedido/{pedidoId}", notificacaoController::findByPedido);
        app.get("/notificacoes/tipo/{tipo}", notificacaoController::findByTipo);
        app.get("/notificacoes/periodo", notificacaoController::findByPeriodo);
        app.get("/notificacoes/count/cliente/{clienteId}", notificacaoController::countByCliente);
        app.get("/notificacoes/count/tipo/{tipo}", notificacaoController::countByTipo);
        app.delete("/notificacoes/cleanup/{days}", notificacaoController::removeOldNotifications);
    }
    
    /**
     * JWT Authentication Middleware
     */
    private static final Handler jwtMiddleware = ctx -> {
        try {
            String authHeader = ctx.header("Authorization");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.status(HttpStatus.UNAUTHORIZED);
                ctx.json(Map.of("error", "Token JWT não fornecido"));
                return;
            }
            
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            
            // Validate token and extract user ID
            var userId = jwtService.extractUserId(token);
            var email = jwtService.extractEmail(token);
            var role = jwtService.extractRole(token);
            
            // Add user info to context attributes
            ctx.attribute("userId", userId.toString());
            ctx.attribute("userEmail", email);
            ctx.attribute("userRole", role.name());
            
        } catch (Exception e) {
            ctx.status(HttpStatus.UNAUTHORIZED);
            ctx.json(Map.of("error", "Token JWT inválido"));
        }
    };
    
    /**
     * Authorization Middleware - Requires specific role
     */
    private static Handler requireRole(String requiredRole) {
        return ctx -> {
            String userRole = ctx.attribute("userRole");
            
            if (userRole == null) {
                ctx.status(HttpStatus.UNAUTHORIZED);
                ctx.json(Map.of("error", "Usuário não autenticado"));
                return;
            }
            
            if (!requiredRole.equals(userRole)) {
                ctx.status(HttpStatus.FORBIDDEN);
                ctx.json(Map.of(
                    "error", "Acesso negado", 
                    "message", "Operação requer permissão de " + requiredRole,
                    "userRole", userRole
                ));
                return;
            }
        };
    }
    
    /**
     * Authorization Middleware - Requires MANAGER role
     */
    private static final Handler requireManager = requireRole("MANAGER");
}