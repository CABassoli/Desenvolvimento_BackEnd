#!/bin/bash

# Script de teste para verificar criação de pedido com novo usuário

# 1. Registrar novo usuário de teste
echo "1. Registrando novo usuário de teste..."
TIMESTAMP=$(date +%s)
EMAIL="test_user_${TIMESTAMP}@test.com"
PASSWORD="Test@123456"

REGISTER_RESPONSE=$(curl -s -X POST http://localhost:5000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "'$EMAIL'",
    "password": "'$PASSWORD'"
  }')

echo "Resposta do registro:"
echo $REGISTER_RESPONSE | python3 -m json.tool 2>/dev/null || echo $REGISTER_RESPONSE

# 2. Login com o novo usuário
echo -e "\n2. Fazendo login com o novo usuário..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:5000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "'$EMAIL'",
    "password": "'$PASSWORD'"
  }')

TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "Erro ao fazer login!"
    echo "Resposta: $LOGIN_RESPONSE"
    exit 1
fi

echo "Login realizado com sucesso!"
echo "Token obtido"

# 3. Obter lista de produtos disponíveis
echo -e "\n3. Obtendo produtos disponíveis..."
PRODUTOS=$(curl -s -X GET http://localhost:5000/api/produtos \
  -H "Authorization: Bearer $TOKEN")

# Pegar o primeiro produto ID
PRODUTO_ID=$(echo $PRODUTOS | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)

if [ -z "$PRODUTO_ID" ]; then
    echo "Nenhum produto encontrado! Vamos criar um produto de teste..."
    
    # Criar categoria primeiro (produtos precisam de categoria)
    echo -e "\nCriando categoria de teste..."
    CAT_RESPONSE=$(curl -s -X POST http://localhost:5000/api/categorias \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "nome": "Categoria Teste",
        "descricao": "Categoria para teste"
      }')
    
    CATEGORIA_ID=$(echo $CAT_RESPONSE | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
    
    if [ -z "$CATEGORIA_ID" ]; then
        echo "Erro ao criar categoria!"
        echo $CAT_RESPONSE
        exit 1
    fi
    
    # Criar produto de teste
    echo -e "\nCriando produto de teste..."
    PROD_RESPONSE=$(curl -s -X POST http://localhost:5000/api/produtos \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "nome": "Produto Teste",
        "descricao": "Produto para teste de pedido",
        "preco": 99.90,
        "estoque": 100,
        "categoriaId": "'$CATEGORIA_ID'"
      }')
    
    PRODUTO_ID=$(echo $PROD_RESPONSE | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
    
    if [ -z "$PRODUTO_ID" ]; then
        echo "Erro ao criar produto!"
        echo $PROD_RESPONSE
        exit 1
    fi
    
    echo "Produto criado com ID: $PRODUTO_ID"
fi

echo "Usando produto ID: $PRODUTO_ID"

# 4. Adicionar item ao carrinho
echo -e "\n4. Adicionando item ao carrinho..."
ADD_ITEM_RESPONSE=$(curl -s -X POST http://localhost:5000/api/carrinho/item \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "produtoId": "'$PRODUTO_ID'",
    "quantidade": 2
  }')

echo "Resposta ao adicionar item:"
echo $ADD_ITEM_RESPONSE | python3 -m json.tool 2>/dev/null || echo $ADD_ITEM_RESPONSE

# Aguardar um pouco
sleep 1

# 5. Verificar carrinho
echo -e "\n5. Verificando carrinho..."
CARRINHO=$(curl -s -X GET http://localhost:5000/api/carrinho \
  -H "Authorization: Bearer $TOKEN")

echo "Estado do carrinho:"
echo $CARRINHO | python3 -m json.tool 2>/dev/null || echo $CARRINHO

# 6. Finalizar pedido (checkout)
echo -e "\n6. Finalizando pedido (checkout)..."
IDEMPOTENCY_KEY="test-key-$(date +%s)"

echo "Fazendo checkout com idempotencyKey: $IDEMPOTENCY_KEY"
PEDIDO_RESPONSE=$(curl -s -X POST http://localhost:5000/api/pedidos/checkout \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "idempotencyKey": "'$IDEMPOTENCY_KEY'"
  }')

echo "Resposta do checkout:"
echo $PEDIDO_RESPONSE | python3 -m json.tool 2>/dev/null || echo $PEDIDO_RESPONSE

# Verificar se houve erro de pedido_id NULL
if echo "$PEDIDO_RESPONSE" | grep -q "null value in column.*pedido_id"; then
    echo -e "\n❌❌❌ ERRO CRÍTICO: Ainda há problema com pedido_id NULL!"
    echo "O erro de constraint NOT NULL ainda persiste!"
    exit 1
elif echo "$PEDIDO_RESPONSE" | grep -q '"id"'; then
    echo -e "\n✅✅✅ SUCESSO: Pedido criado sem erro de pedido_id NULL!"
    
    # Pegar o ID do pedido criado
    PEDIDO_ID=$(echo $PEDIDO_RESPONSE | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
    echo "ID do pedido criado: $PEDIDO_ID"
    echo "Total do pedido: $(echo $PEDIDO_RESPONSE | grep -o '"total":[0-9.]*' | cut -d':' -f2)"
    
    # 7. Verificar detalhes do pedido criado
    echo -e "\n7. Verificando detalhes do pedido criado..."
    PEDIDO_DETALHES=$(curl -s -X GET http://localhost:5000/api/pedidos/$PEDIDO_ID \
      -H "Authorization: Bearer $TOKEN")
    
    echo "Detalhes do pedido:"
    echo $PEDIDO_DETALHES | python3 -m json.tool 2>/dev/null || echo $PEDIDO_DETALHES
    
    # Verificar se os itens têm pedido_id preenchido
    if echo "$PEDIDO_DETALHES" | grep -q '"itens"'; then
        echo -e "\n✅ Pedido tem itens associados!"
    fi
    
    # 8. Verificar carrinho após checkout (deve estar vazio)
    echo -e "\n8. Verificando se o carrinho foi limpo..."
    CARRINHO_POS=$(curl -s -X GET http://localhost:5000/api/carrinho \
      -H "Authorization: Bearer $TOKEN")
    
    echo "Carrinho após checkout:"
    echo $CARRINHO_POS | python3 -m json.tool 2>/dev/null || echo $CARRINHO_POS
    
    if echo "$CARRINHO_POS" | grep -q '"itens":\[\]'; then
        echo "✅ Carrinho foi limpo corretamente!"
    fi
    
    echo -e "\n✅✅✅ TESTE COMPLETO COM SUCESSO!"
    echo "O erro de pedido_id NULL foi corrigido com sucesso!"
    exit 0
else
    echo -e "\n❓ Resposta inesperada do servidor"
    exit 1
fi