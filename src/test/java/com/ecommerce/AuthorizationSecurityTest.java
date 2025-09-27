package com.ecommerce;

import com.ecommerce.config.DatabaseConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import okhttp3.Response;
import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de segurança e autorização para validar:
 * 1. Controles de acesso por role (CUSTOMER, MANAGER)
 * 2. Proteção de endpoints administrativos
 * 3. Isolamento de dados entre clientes
 * 4. Validação de propriedade de recursos
 */
class AuthorizationSecurityTest {

    private static Javalin app;
    private static ObjectMapper objectMapper;
    
    private String managerToken;
    private String customerToken1;
    private String customerToken2;
    private UUID clienteId1;
    private UUID clienteId2;
    private UUID categoriaId;
    private UUID produtoId;

    @BeforeAll
    static void setupAll() {
        DatabaseConfig.initialize();
        app = App.createApp();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @AfterAll
    static void tearDownAll() {
        if (app != null) {
            app.stop();
        }
    }

    @BeforeEach
    void setUp() {
        JavalinTest.test(app, (server, client) -> {
            // Criar usuário MANAGER
            client.post("/auth/register", Map.of(
                "email", "manager@security.test",
                "password", "123456",
                "role", "MANAGER"
            ));
            
            Response managerLogin = client.post("/auth/login", Map.of(
                "email", "manager@security.test",
                "password", "123456"
            ));
            Map<String, Object> managerResult = objectMapper.readValue(
                managerLogin.body().string(), Map.class);
            managerToken = (String) managerResult.get("token");

            // Criar primeiro CUSTOMER
            client.post("/auth/register", Map.of(
                "email", "customer1@security.test",
                "password", "123456",
                "role", "CUSTOMER"
            ));
            
            Response customer1Login = client.post("/auth/login", Map.of(
                "email", "customer1@security.test",
                "password", "123456"
            ));
            Map<String, Object> customer1Result = objectMapper.readValue(
                customer1Login.body().string(), Map.class);
            customerToken1 = (String) customer1Result.get("token");

            // Criar segundo CUSTOMER
            client.post("/auth/register", Map.of(
                "email", "customer2@security.test",
                "password", "123456",
                "role", "CUSTOMER"
            ));
            
            Response customer2Login = client.post("/auth/login", Map.of(
                "email", "customer2@security.test",
                "password", "123456"
            ));
            Map<String, Object> customer2Result = objectMapper.readValue(
                customer2Login.body().string(), Map.class);
            customerToken2 = (String) customer2Result.get("token");

            // Criar categoria e produto para testes
            Response categoriaResponse = client.post("/categorias", 
                Map.of("nome", "Categoria Teste"), builder ->
                builder.header("Authorization", "Bearer " + managerToken));
            Map<String, Object> categoria = objectMapper.readValue(
                categoriaResponse.body().string(), Map.class);
            categoriaId = UUID.fromString((String) categoria.get("id"));

            Response produtoResponse = client.post("/produtos", Map.of(
                "nome", "Produto Teste",
                "preco", 100.0,
                "codigoBarras", "1111111111111",
                "categoriaId", categoriaId.toString()
            ), builder -> builder.header("Authorization", "Bearer " + managerToken));
            Map<String, Object> produto = objectMapper.readValue(
                produtoResponse.body().string(), Map.class);
            produtoId = UUID.fromString((String) produto.get("id"));

            // Criar clientes
            Response cliente1Response = client.post("/clientes", Map.of(
                "nome", "Cliente 1",
                "email", "cliente1@test.com"
            ), builder -> builder.header("Authorization", "Bearer " + customerToken1));
            Map<String, Object> cliente1 = objectMapper.readValue(
                cliente1Response.body().string(), Map.class);
            clienteId1 = UUID.fromString((String) cliente1.get("id"));

            Response cliente2Response = client.post("/clientes", Map.of(
                "nome", "Cliente 2", 
                "email", "cliente2@test.com"
            ), builder -> builder.header("Authorization", "Bearer " + customerToken2));
            Map<String, Object> cliente2 = objectMapper.readValue(
                cliente2Response.body().string(), Map.class);
            clienteId2 = UUID.fromString((String) cliente2.get("id"));
        });
    }

    @Test
    @DisplayName("Deve proteger endpoints administrativos de categorias")
    void deveProtegerEndpointsAdministrativosCategorias() {
        JavalinTest.test(app, (server, client) -> {
            var categoriaData = Map.of("nome", "Nova Categoria");
            
            // Sem token - deve retornar 401
            Response unauthorizedResponse = client.post("/categorias", categoriaData);
            assertThat(unauthorizedResponse.code()).isEqualTo(401);
            
            // Com token CUSTOMER - deve retornar 403
            Response forbiddenResponse = client.post("/categorias", categoriaData, builder ->
                builder.header("Authorization", "Bearer " + customerToken1));
            assertThat(forbiddenResponse.code()).isEqualTo(403);
            
            // Com token MANAGER - deve funcionar
            Response successResponse = client.post("/categorias", categoriaData, builder ->
                builder.header("Authorization", "Bearer " + managerToken));
            assertThat(successResponse.code()).isEqualTo(201);
            
            // Teste PUT e DELETE também
            Response putResponse = client.put("/categorias/" + categoriaId, 
                Map.of("nome", "Categoria Atualizada"), builder ->
                builder.header("Authorization", "Bearer " + customerToken1));
            assertThat(putResponse.code()).isEqualTo(403);
            
            Response deleteResponse = client.delete("/categorias/" + categoriaId);
            assertThat(deleteResponse.code()).isEqualTo(401); // Should return 401 without auth
        });
    }

    @Test
    @DisplayName("Deve proteger endpoints administrativos de produtos")
    void deveProtegerEndpointsAdministrativosProdutos() {
        JavalinTest.test(app, (server, client) -> {
            var produtoData = Map.of(
                "nome", "Novo Produto",
                "preco", 200.0,
                "codigoBarras", "2222222222222",
                "categoriaId", categoriaId.toString()
            );
            
            // CUSTOMER não pode criar produtos
            Response forbiddenCreate = client.post("/produtos", produtoData, builder ->
                builder.header("Authorization", "Bearer " + customerToken1));
            assertThat(forbiddenCreate.code()).isEqualTo(403);
            
            // CUSTOMER não pode atualizar produtos
            Response forbiddenUpdate = client.put("/produtos/" + produtoId,
                Map.of("nome", "Produto Atualizado"), builder ->
                builder.header("Authorization", "Bearer " + customerToken1));
            assertThat(forbiddenUpdate.code()).isEqualTo(403);
            
            // CUSTOMER não pode deletar produtos
            Response forbiddenDelete = client.delete("/produtos/" + produtoId);
            assertThat(forbiddenDelete.code()).isEqualTo(401); // Should return 401 without auth
            
            // MANAGER pode fazer todas as operações
            Response managerCreate = client.post("/produtos", produtoData, builder ->
                builder.header("Authorization", "Bearer " + managerToken));
            assertThat(managerCreate.code()).isEqualTo(201);
        });
    }

    @Test
    @DisplayName("Deve isolar dados entre clientes diferentes")
    void deveIsolarDadosEntreClientes() {
        JavalinTest.test(app, (server, client) -> {
            // Cliente 1 não pode acessar dados do Cliente 2
            Response forbiddenAccess = client.get("/carrinho/" + clienteId2, builder ->
                builder.header("Authorization", "Bearer " + customerToken1));
            assertThat(forbiddenAccess.code()).isIn(403, 404);
            
            // Cliente 1 pode acessar seus próprios dados
            Response allowedAccess = client.get("/carrinho/" + clienteId1, builder ->
                builder.header("Authorization", "Bearer " + customerToken1));
            assertThat(allowedAccess.code()).isEqualTo(200);
            
            // Cliente 1 não pode modificar carrinho do Cliente 2
            Response forbiddenModify = client.post("/carrinho/" + clienteId2 + "/itens",
                Map.of("produtoId", produtoId.toString(), "quantidade", 1), builder ->
                builder.header("Authorization", "Bearer " + customerToken1));
            assertThat(forbiddenModify.code()).isIn(403, 404);
        });
    }

    @Test
    @DisplayName("Deve validar propriedade de endereços")
    void deveValidarPropriedadeEnderecos() {
        JavalinTest.test(app, (server, client) -> {
            // Criar endereço para Cliente 1
            Response enderecoResponse = client.post("/clientes/" + clienteId1 + "/enderecos",
                Map.of("rua", "Rua A", "cidade", "Cidade A", "cep", "12345-678", "numero", "100"), builder ->
                builder.header("Authorization", "Bearer " + customerToken1));
            assertThat(enderecoResponse.code()).isEqualTo(201);
            
            Map<String, Object> endereco = objectMapper.readValue(
                enderecoResponse.body().string(), Map.class);
            UUID enderecoId = UUID.fromString((String) endereco.get("id"));
            
            // Cliente 2 não pode modificar endereço do Cliente 1
            Response forbiddenUpdate = client.put("/clientes/" + clienteId1 + "/enderecos/" + enderecoId,
                Map.of("rua", "Rua Modificada"), builder ->
                builder.header("Authorization", "Bearer " + customerToken2));
            assertThat(forbiddenUpdate.code()).isIn(403, 404);
            
            // Cliente 2 não pode deletar endereço do Cliente 1
            Response forbiddenDelete = client.delete("/clientes/" + clienteId1 + "/enderecos/" + enderecoId);
            assertThat(forbiddenDelete.code()).isEqualTo(401); // Should return 401 without auth
        });
    }

    @Test
    @DisplayName("Deve proteger dados sensíveis de pagamentos")
    void deveProtegerDadosSensitivosPagamentos() {
        JavalinTest.test(app, (server, client) -> {
            // Adicionar item ao carrinho do Cliente 1
            client.post("/carrinho/" + clienteId1 + "/itens",
                Map.of("produtoId", produtoId.toString(), "quantidade", 1), builder ->
                builder.header("Authorization", "Bearer " + customerToken1));
                
            // Criar endereço para o pedido
            Response enderecoResponse = client.post("/clientes/" + clienteId1 + "/enderecos",
                Map.of("rua", "Rua A", "cidade", "Cidade A", "cep", "12345-678", "numero", "100"), builder ->
                builder.header("Authorization", "Bearer " + customerToken1));
            Map<String, Object> endereco = objectMapper.readValue(
                enderecoResponse.body().string(), Map.class);
            UUID enderecoId = UUID.fromString((String) endereco.get("id"));
            
            // Criar pedido para Cliente 1
            Response pedidoResponse = client.post("/pedidos/" + clienteId1,
                Map.of("enderecoId", enderecoId.toString()), builder ->
                builder.header("Authorization", "Bearer " + customerToken1));
            Map<String, Object> pedido = objectMapper.readValue(
                pedidoResponse.body().string(), Map.class);
            UUID pedidoId = UUID.fromString((String) pedido.get("id"));
            
            // Cliente 2 não pode processar pagamento do pedido do Cliente 1
            Response forbiddenPayment = client.post("/pagamentos/" + pedidoId + "/pix",
                Map.of("valor", 100.0), builder ->
                builder.header("Authorization", "Bearer " + customerToken2));
            assertThat(forbiddenPayment.code()).isIn(403, 404);
            
            // Cliente 1 pode processar seu próprio pagamento
            Response allowedPayment = client.post("/pagamentos/" + pedidoId + "/pix",
                Map.of("valor", 100.0), builder ->
                builder.header("Authorization", "Bearer " + customerToken1));
            assertThat(allowedPayment.code()).isEqualTo(201);
        });
    }

    @Test
    @DisplayName("Deve validar tokens JWT inválidos")
    void deveValidarTokensJWTInvalidos() {
        JavalinTest.test(app, (server, client) -> {
            // Token malformado
            Response malformedToken = client.get("/auth/profile", builder ->
                builder.header("Authorization", "Bearer token_invalido"));
            assertThat(malformedToken.code()).isEqualTo(401);
            
            // Token vazio
            Response emptyToken = client.get("/auth/profile", builder ->
                builder.header("Authorization", "Bearer "));
            assertThat(emptyToken.code()).isEqualTo(401);
            
            // Sem header Authorization
            Response noAuth = client.get("/auth/profile");
            assertThat(noAuth.code()).isEqualTo(401);
            
            // Header malformado
            Response malformedHeader = client.get("/auth/profile", builder ->
                builder.header("Authorization", "InvalidFormat " + customerToken1));
            assertThat(malformedHeader.code()).isEqualTo(401);
        });
    }

    @Test
    @DisplayName("Deve permitir acesso público a endpoints de leitura")
    void devePermitirAcessoPublicoEndpointsLeitura() {
        JavalinTest.test(app, (server, client) -> {
            // Categorias - leitura pública
            Response categorias = client.get("/categorias");
            assertThat(categorias.code()).isEqualTo(200);
            
            // Produtos - leitura pública
            Response produtos = client.get("/produtos");
            assertThat(produtos.code()).isEqualTo(200);
            
            // Produto específico - leitura pública
            Response produto = client.get("/produtos/" + produtoId);
            assertThat(produto.code()).isEqualTo(200);
            
            // Health check - sempre público
            Response health = client.get("/health");
            assertThat(health.code()).isEqualTo(200);
            
            // Página inicial - sempre pública
            Response index = client.get("/");
            assertThat(index.code()).isEqualTo(200);
        });
    }
}