package com.ecommerce;

import com.ecommerce.config.DatabaseConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import okhttp3.Response;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração para validar o fluxo completo de e-commerce:
 * 1. Registro de usuário e autenticação
 * 2. Criação de categorias e produtos 
 * 3. Gestão de clientes e endereços
 * 4. Operações de carrinho de compras
 * 5. Processamento de pedidos
 * 6. Sistema de pagamentos
 * 7. Notificações
 */
class ECommerceIntegrationTest {

    private static Javalin app;
    private static ObjectMapper objectMapper;
    
    // Dados de teste
    private String managerToken;
    private String customerToken;
    private UUID categoriaId;
    private UUID produtoId;
    private UUID clienteId;
    private UUID enderecoId;
    private UUID pedidoId;

    @BeforeAll
    static void setupAll() {
        // Configurar banco de teste
        DatabaseConfig.initialize();
        
        // Configurar aplicação
        app = App.createApp();
        
        // Configurar ObjectMapper
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @AfterAll
    static void tearDownAll() {
        if (app != null) {
            app.stop();
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. Deve registrar usuário MANAGER e fazer login")
    void deveRegistrarManagerEFazerLogin() {
        JavalinTest.test(app, (server, client) -> {
            // Registrar manager
            var registerData = Map.of(
                "email", "admin@test.com",
                "password", "123456",
                "role", "MANAGER"
            );
            
            Response registerResponse = client.post("/auth/register", registerData);
            assertThat(registerResponse.code()).isEqualTo(201);
            
            // Fazer login
            var loginData = Map.of(
                "email", "admin@test.com", 
                "password", "123456"
            );
            
            Response loginResponse = client.post("/auth/login", loginData);
            assertThat(loginResponse.code()).isEqualTo(200);
            
            Map<String, Object> loginResult = objectMapper.readValue(
                loginResponse.body().string(), Map.class);
            managerToken = (String) loginResult.get("token");
            
            assertThat(managerToken).isNotNull();
        });
    }

    @Test
    @Order(2)
    @DisplayName("2. Deve registrar usuário CUSTOMER e fazer login")
    void deveRegistrarCustomerEFazerLogin() {
        JavalinTest.test(app, (server, client) -> {
            // Registrar customer
            var registerData = Map.of(
                "email", "customer@test.com",
                "password", "123456",
                "role", "CUSTOMER"
            );
            
            Response registerResponse = client.post("/auth/register", registerData);
            assertThat(registerResponse.code()).isEqualTo(201);
            
            // Fazer login
            var loginData = Map.of(
                "email", "customer@test.com",
                "password", "123456"
            );
            
            Response loginResponse = client.post("/auth/login", loginData);
            assertThat(loginResponse.code()).isEqualTo(200);
            
            Map<String, Object> loginResult = objectMapper.readValue(
                loginResponse.body().string(), Map.class);
            customerToken = (String) loginResult.get("token");
            
            assertThat(customerToken).isNotNull();
        });
    }

    @Test
    @Order(3)
    @DisplayName("3. Deve criar categoria (somente MANAGER)")
    void deveCriarCategoria() {
        JavalinTest.test(app, (server, client) -> {
            // Tentar criar categoria sem token (deve falhar)
            var categoriaData = Map.of("nome", "Eletrônicos");
            
            Response unauthorizedResponse = client.post("/categorias", categoriaData);
            assertThat(unauthorizedResponse.code()).isEqualTo(401);
            
            // Tentar criar categoria com token CUSTOMER (deve falhar)
            Response forbiddenResponse = client.post("/categorias", categoriaData, builder -> 
                builder.header("Authorization", "Bearer " + customerToken));
            assertThat(forbiddenResponse.code()).isEqualTo(403);
            
            // Criar categoria com token MANAGER (deve funcionar)
            Response successResponse = client.post("/categorias", categoriaData, builder ->
                builder.header("Authorization", "Bearer " + managerToken));
            assertThat(successResponse.code()).isEqualTo(201);
            
            Map<String, Object> categoria = objectMapper.readValue(
                successResponse.body().string(), Map.class);
            categoriaId = UUID.fromString((String) categoria.get("id"));
            
            assertThat(categoria.get("nome")).isEqualTo("Eletrônicos");
        });
    }

    @Test
    @Order(4)
    @DisplayName("4. Deve criar produto (somente MANAGER)")
    void deveCriarProduto() {
        JavalinTest.test(app, (server, client) -> {
            var produtoData = Map.of(
                "nome", "Smartphone XYZ",
                "preco", 899.99,
                "codigoBarras", "1234567890123",
                "categoriaId", categoriaId.toString()
            );
            
            // Criar produto com token MANAGER
            Response response = client.post("/produtos", produtoData, builder ->
                builder.header("Authorization", "Bearer " + managerToken));
            assertThat(response.code()).isEqualTo(201);
            
            Map<String, Object> produto = objectMapper.readValue(
                response.body().string(), Map.class);
            produtoId = UUID.fromString((String) produto.get("id"));
            
            assertThat(produto.get("nome")).isEqualTo("Smartphone XYZ");
            assertThat(produto.get("preco")).isEqualTo(899.99);
        });
    }

    @Test
    @Order(5)
    @DisplayName("5. Deve listar categorias e produtos (acesso público)")
    void deveListarCategoriasEProdutos() {
        JavalinTest.test(app, (server, client) -> {
            // Listar categorias sem autenticação
            Response categoriasResponse = client.get("/categorias");
            assertThat(categoriasResponse.code()).isEqualTo(200);
            
            // Listar produtos sem autenticação
            Response produtosResponse = client.get("/produtos");
            assertThat(produtosResponse.code()).isEqualTo(200);
            
            // Buscar produto específico
            Response produtoResponse = client.get("/produtos/" + produtoId);
            assertThat(produtoResponse.code()).isEqualTo(200);
        });
    }

    @Test
    @Order(6)
    @DisplayName("6. Deve criar cliente")
    void deveCriarCliente() {
        JavalinTest.test(app, (server, client) -> {
            var clienteData = Map.of(
                "nome", "João Silva",
                "email", "joao@test.com"
            );
            
            Response response = client.post("/clientes", clienteData, builder ->
                builder.header("Authorization", "Bearer " + customerToken));
            assertThat(response.code()).isEqualTo(201);
            
            Map<String, Object> cliente = objectMapper.readValue(
                response.body().string(), Map.class);
            clienteId = UUID.fromString((String) cliente.get("id"));
            
            assertThat(cliente.get("nome")).isEqualTo("João Silva");
            assertThat(cliente.get("email")).isEqualTo("joao@test.com");
        });
    }

    @Test
    @Order(7)
    @DisplayName("7. Deve criar endereço para cliente")
    void deveCriarEndereco() {
        JavalinTest.test(app, (server, client) -> {
            var enderecoData = Map.of(
                "rua", "Rua das Flores, 123",
                "cidade", "São Paulo",
                "cep", "01234-567",
                "numero", "123"
            );
            
            Response response = client.post("/clientes/" + clienteId + "/enderecos", 
                enderecoData, builder -> builder.header("Authorization", "Bearer " + customerToken));
            assertThat(response.code()).isEqualTo(201);
            
            Map<String, Object> endereco = objectMapper.readValue(
                response.body().string(), Map.class);
            enderecoId = UUID.fromString((String) endereco.get("id"));
            
            assertThat(endereco.get("rua")).isEqualTo("Rua das Flores, 123");
            assertThat(endereco.get("cidade")).isEqualTo("São Paulo");
        });
    }

    @Test
    @Order(8)
    @DisplayName("8. Deve adicionar item ao carrinho")
    void deveAdicionarItemAoCarrinho() {
        JavalinTest.test(app, (server, client) -> {
            var itemData = Map.of(
                "produtoId", produtoId.toString(),
                "quantidade", 2
            );
            
            Response response = client.post("/carrinho/" + clienteId + "/itens",
                itemData, builder -> builder.header("Authorization", "Bearer " + customerToken));
            assertThat(response.code()).isEqualTo(201);
            
            // Verificar total do carrinho
            Response totalResponse = client.get("/carrinho/" + clienteId + "/total", builder ->
                builder.header("Authorization", "Bearer " + customerToken));
            assertThat(totalResponse.code()).isEqualTo(200);
            
            Map<String, Object> total = objectMapper.readValue(
                totalResponse.body().string(), Map.class);
            
            // 2 * 899.99 = 1799.98
            assertThat(total.get("total")).isEqualTo(1799.98);
        });
    }

    @Test
    @Order(9)
    @DisplayName("9. Deve criar pedido")
    void deveCriarPedido() {
        JavalinTest.test(app, (server, client) -> {
            var pedidoData = Map.of("enderecoId", enderecoId.toString());
            
            Response response = client.post("/pedidos/" + clienteId, pedidoData, builder ->
                builder.header("Authorization", "Bearer " + customerToken));
            assertThat(response.code()).isEqualTo(201);
            
            Map<String, Object> pedido = objectMapper.readValue(
                response.body().string(), Map.class);
            pedidoId = UUID.fromString((String) pedido.get("id"));
            
            assertThat(pedido.get("status")).isEqualTo("PROCESSANDO");
            assertThat(pedido.get("valorTotal")).isEqualTo(1799.98);
        });
    }

    @Test
    @Order(10)
    @DisplayName("10. Deve processar pagamento PIX")
    void deveProcessarPagamentoPix() {
        JavalinTest.test(app, (server, client) -> {
            var pixData = Map.of("valor", 1799.98);
            
            Response response = client.post("/pagamentos/" + pedidoId + "/pix",
                pixData, builder -> builder.header("Authorization", "Bearer " + customerToken));
            assertThat(response.code()).isEqualTo(201);
            
            Map<String, Object> pagamento = objectMapper.readValue(
                response.body().string(), Map.class);
            
            assertThat(pagamento.get("valor")).isEqualTo(1799.98);
            assertThat(pagamento.get("txid")).isNotNull();
        });
    }

    @Test
    @Order(11)
    @DisplayName("11. Deve listar notificações do cliente")
    void deveListarNotificacoes() {
        JavalinTest.test(app, (server, client) -> {
            Response response = client.get("/notificacoes/cliente/" + clienteId, builder ->
                builder.header("Authorization", "Bearer " + customerToken));
            assertThat(response.code()).isEqualTo(200);
            
            // Deve ter pelo menos uma notificação de confirmação do pedido
            var notificacoes = objectMapper.readValue(
                response.body().string(), Object[].class);
            assertThat(notificacoes.length).isGreaterThan(0);
        });
    }

    @Test
    @Order(12)
    @DisplayName("12. Deve validar controle de acesso")
    void deveValidarControleDeAcesso() {
        JavalinTest.test(app, (server, client) -> {
            // Tentar acessar perfil sem token
            Response unauthorizedResponse = client.get("/auth/profile");
            assertThat(unauthorizedResponse.code()).isEqualTo(401);
            
            // Acessar perfil com token válido
            Response authorizedResponse = client.get("/auth/profile", builder ->
                builder.header("Authorization", "Bearer " + customerToken));
            assertThat(authorizedResponse.code()).isEqualTo(200);
            
            // Customer não pode criar produtos
            var produtoData = Map.of(
                "nome", "Produto Inválido",
                "preco", 99.99,
                "codigoBarras", "9999999999999",
                "categoriaId", categoriaId.toString()
            );
            
            Response forbiddenResponse = client.post("/produtos", produtoData, builder ->
                builder.header("Authorization", "Bearer " + customerToken));
            assertThat(forbiddenResponse.code()).isEqualTo(403);
        });
    }

    @Test
    @Order(13)
    @DisplayName("13. Deve validar endpoints de health e sistema")
    void deveValidarEndpointsDoSistema() {
        JavalinTest.test(app, (server, client) -> {
            // Health check
            Response healthResponse = client.get("/health");
            assertThat(healthResponse.code()).isEqualTo(200);
            
            Map<String, Object> health = objectMapper.readValue(
                healthResponse.body().string(), Map.class);
            assertThat(health.get("status")).isEqualTo("OK");
            
            // Página inicial
            Response indexResponse = client.get("/");
            assertThat(indexResponse.code()).isEqualTo(200);
            assertThat(indexResponse.body().string()).contains("E-commerce API");
        });
    }
}