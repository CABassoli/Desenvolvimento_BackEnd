# 🛒 E-commerce Order Management API

Uma API REST completa para gerenciamento de pedidos de e-commerce construída com **Java 21**, **Javalin** e **JPA/Hibernate**.

## 🚀 Início Rápido

### Executar a Aplicação
```bash
./gradlew run
```

A API estará disponível em: **http://localhost:5000**

### 📊 Ferramentas de Desenvolvimento
- **Health Check**: http://localhost:5000/health  
- **Console H2**: http://localhost:5000/h2-console
  - JDBC URL: `jdbc:h2:mem:ecommerce`
  - User: `SA`
  - Password: (vazio)

## 🔐 Autenticação e Autorização

### Registro de Usuário
```bash
# Registrar usuário comum
curl -X POST http://localhost:5000/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"123456","role":"CUSTOMER"}'

# Registrar administrador
curl -X POST http://localhost:5000/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"123456","role":"MANAGER"}'
```

### Login
```bash
curl -X POST http://localhost:5000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"123456"}'
```

**Resposta:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "email": "admin@example.com",
    "role": "MANAGER"
  }
}
```

### Usar Token JWT
```bash
# Incluir o token no header Authorization
curl -H "Authorization: Bearer SEU_TOKEN_JWT" \
  http://localhost:5000/auth/profile
```

## 📋 Principais Endpoints

### 🏷️ Categorias
```bash
# Listar categorias (público)
GET /categorias

# Criar categoria (MANAGER apenas)
POST /categorias
# Body: {"nome": "Eletrônicos"}

# Atualizar categoria (MANAGER apenas)  
PUT /categorias/{id}

# Deletar categoria (MANAGER apenas)
DELETE /categorias/{id}
```

### 📦 Produtos
```bash
# Listar produtos (público)
GET /produtos

# Buscar por categoria (público)
GET /produtos/categoria/{categoriaId}

# Buscar por nome (público)
GET /produtos/buscar/{nome}

# Criar produto (MANAGER apenas)
POST /produtos
# Body: {"nome": "iPhone 15", "preco": 5999.99, "codigoBarras": "1234567890123", "categoriaId": "categoria-uuid"}

# Atualizar produto (MANAGER apenas)
PUT /produtos/{id}

# Deletar produto (MANAGER apenas)
DELETE /produtos/{id}
```

### 👥 Clientes
```bash
# Listar clientes (autenticado)
GET /clientes

# Criar cliente (autenticado)
POST /clientes
# Body: {"nome": "João Silva", "email": "joao@example.com"}

# Buscar cliente (autenticado)
GET /clientes/{id}
```

### 📍 Endereços
```bash
# Listar endereços do cliente (autenticado)
GET /clientes/{clienteId}/enderecos

# Adicionar endereço (autenticado)
POST /clientes/{clienteId}/enderecos
# Body: {"rua": "Rua A, 123", "cidade": "São Paulo", "cep": "01234-567", "numero": "123"}

# Atualizar endereço (autenticado)
PUT /clientes/{clienteId}/enderecos/{enderecoId}

# Deletar endereço (autenticado)
DELETE /clientes/{clienteId}/enderecos/{enderecoId}
```

### 🛒 Carrinho de Compras
```bash
# Ver carrinho (autenticado)
GET /carrinho/{clienteId}

# Adicionar item ao carrinho (autenticado)
POST /carrinho/{clienteId}/itens
# Body: {"produtoId": "produto-uuid", "quantidade": 2}

# Atualizar quantidade (autenticado)
PUT /carrinho/{clienteId}/itens/{itemId}
# Body: {"quantidade": 3}

# Remover item (autenticado)
DELETE /carrinho/{clienteId}/itens/{itemId}

# Ver total do carrinho (autenticado)
GET /carrinho/{clienteId}/total
```

### 📄 Pedidos
```bash
# Criar pedido (autenticado)
POST /pedidos/{clienteId}
# Body: {"enderecoId": "endereco-uuid"}

# Listar pedidos do cliente (autenticado)
GET /pedidos/cliente/{clienteId}

# Ver pedido específico (autenticado)
GET /pedidos/{id}

# Atualizar status (autenticado)
PUT /pedidos/{id}/status/{status}
# Status: PROCESSANDO, ENVIADO, ENTREGUE, CANCELADO
```

### 💳 Pagamentos
```bash
# Pagamento PIX (autenticado)
POST /pagamentos/{pedidoId}/pix
# Body: {"txid": "PIX123456789"}

# Pagamento Cartão (autenticado)
POST /pagamentos/{pedidoId}/cartao
# Body: {"tokenCartao": "tok_123", "bandeira": "VISA"}

# Pagamento Boleto (autenticado)
POST /pagamentos/{pedidoId}/boleto
# Body: {"linhaDigitavel": "12345.67890 12345.67890..."}

# Confirmar pagamento boleto
PUT /pagamentos/boleto/confirmar/{linhaDigitavel}
```

### 🔔 Notificações
```bash
# Ver notificações do cliente (autenticado)
GET /notificacoes/cliente/{clienteId}

# Ver notificações recentes (autenticado)
GET /notificacoes/cliente/{clienteId}/recentes

# Ver notificação específica (autenticado)
GET /notificacoes/{id}
```

## 🔒 Níveis de Autorização

| Endpoint | Público | CUSTOMER | MANAGER |
|----------|---------|----------|---------|
| GET /categorias, /produtos | ✅ | ✅ | ✅ |
| POST /categorias, /produtos | ❌ | ❌ | ✅ |
| PUT/DELETE /categorias, /produtos | ❌ | ❌ | ✅ |
| Carrinho, Pedidos, Clientes | ❌ | ✅ | ✅ |
| Pagamentos, Notificações | ❌ | ✅ | ✅ |

## 📋 Códigos de Status HTTP

- **200 OK**: Operação bem-sucedida
- **201 Created**: Recurso criado com sucesso
- **400 Bad Request**: Dados inválidos no request
- **401 Unauthorized**: Token JWT ausente ou inválido
- **403 Forbidden**: Role inadequada para a operação
- **404 Not Found**: Recurso não encontrado
- **409 Conflict**: Conflito (ex: email já cadastrado)
- **500 Internal Server Error**: Erro interno do servidor

## 🛠️ Tecnologias Utilizadas

- **Java 21** - Linguagem principal
- **Javalin 6.x** - Framework web REST
- **JPA/Hibernate** - ORM para persistência
- **HikariCP** - Pool de conexões
- **H2 Database** - Banco em memória para desenvolvimento
- **Auth0 java-jwt** - Geração e validação de tokens JWT
- **MapStruct** - Mapeamento entre DTOs e entidades
- **Lombok** - Redução de boilerplate
- **Gradle** - Ferramenta de build

## 🏗️ Arquitetura

```
src/main/java/com/ecommerce/
├── App.java              # Configuração principal do Javalin
├── controller/           # Controllers REST
├── service/             # Lógica de negócio
├── repository/          # Acesso a dados
├── domain/              # Entidades JPA
├── dto/                 # Data Transfer Objects
│   ├── request/         # DTOs de entrada
│   └── response/        # DTOs de saída
├── mapper/              # MapStruct mappers
├── config/              # Configurações (DB, JWT)
└── util/                # Utilitários
```

## 📝 Exemplo de Fluxo Completo

```bash
# 1. Registrar admin
curl -X POST http://localhost:5000/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@loja.com","password":"admin123","role":"MANAGER"}'

# 2. Fazer login e obter token
TOKEN=$(curl -X POST http://localhost:5000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@loja.com","password":"admin123"}' | jq -r '.token')

# 3. Criar categoria
curl -X POST http://localhost:5000/categorias \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nome":"Eletrônicos"}'

# 4. Criar produto
curl -X POST http://localhost:5000/produtos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nome":"iPhone 15","preco":5999.99,"codigoBarras":"1234567890123","categoriaId":"categoria-uuid"}'

# 5. Registrar cliente
curl -X POST http://localhost:5000/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"cliente@example.com","password":"123456","role":"CUSTOMER"}'

# 6. Cliente faz login
CLIENT_TOKEN=$(curl -X POST http://localhost:5000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"cliente@example.com","password":"123456"}' | jq -r '.token')

# 7. Criar perfil do cliente
curl -X POST http://localhost:5000/clientes \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nome":"João Silva","email":"cliente@example.com"}'

# 8. Adicionar endereço
curl -X POST http://localhost:5000/clientes/{clienteId}/enderecos \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"rua":"Rua A, 123","cidade":"São Paulo","cep":"01234-567","numero":"123"}'

# 9. Adicionar produto ao carrinho
curl -X POST http://localhost:5000/carrinho/{clienteId}/itens \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"produtoId":"produto-uuid","quantidade":1}'

# 10. Finalizar pedido
curl -X POST http://localhost:5000/pedidos/{clienteId} \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"enderecoId":"endereco-uuid"}'

# 11. Processar pagamento PIX
curl -X POST http://localhost:5000/pagamentos/{pedidoId}/pix \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"txid":"PIX123456789"}'
```

## 🎯 Próximos Passos

- [ ] Implementar testes automatizados
- [ ] Adicionar validações mais robustas
- [ ] Integrar com serviços reais de pagamento
- [ ] Implementar logging estruturado
- [ ] Adicionar métricas e monitoramento
- [ ] Configurar banco de dados persistente para produção

---

**API REST E-commerce** - Sistema completo de gerenciamento de pedidos com autenticação JWT e autorização por roles.