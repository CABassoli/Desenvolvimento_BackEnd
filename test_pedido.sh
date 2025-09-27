#!/bin/bash

# Script de teste para verificar criação de pedido

# 1. Primeiro fazer login para obter token
echo "1. Fazendo login..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:5000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "cleber.bassoli1@gmail.com",
    "password": "Admin@123"
  }')

TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "Erro ao fazer login!"
    echo "Resposta: $LOGIN_RESPONSE"
    exit 1
fi

echo "Login realizado com sucesso!"
echo "Token: $TOKEN"

# 2. Obter lista de produtos
echo -e "\n2. Obtendo lista de produtos..."
PRODUTOS=$(curl -s -X GET http://localhost:5000/api/produtos \
  -H "Authorization: Bearer $TOKEN")

echo "Produtos disponíveis:"
echo $PRODUTOS | python3 -m json.tool 2>/dev/null || echo $PRODUTOS

# Pegar o primeiro produto ID
PRODUTO_ID=$(echo $PRODUTOS | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)

if [ -z "$PRODUTO_ID" ]; then
    echo "Nenhum produto encontrado!"
    exit 1
fi

echo -e "\nUsando produto ID: $PRODUTO_ID"

# 3. Adicionar item ao carrinho
echo -e "\n3. Adicionando item ao carrinho..."
ADD_ITEM_RESPONSE=$(curl -s -X POST http://localhost:5000/api/carrinho/item \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "produtoId": "'$PRODUTO_ID'",
    "quantidade": 2
  }')

echo "Resposta ao adicionar item:"
echo $ADD_ITEM_RESPONSE | python3 -m json.tool 2>/dev/null || echo $ADD_ITEM_RESPONSE

# 4. Verificar carrinho
echo -e "\n4. Verificando carrinho..."
CARRINHO=$(curl -s -X GET http://localhost:5000/api/carrinho \
  -H "Authorization: Bearer $TOKEN")

echo "Estado do carrinho:"
echo $CARRINHO | python3 -m json.tool 2>/dev/null || echo $CARRINHO

# 5. Finalizar pedido (checkout)
echo -e "\n5. Finalizando pedido (checkout)..."
IDEMPOTENCY_KEY=$(uuidgen 2>/dev/null || echo "test-$(date +%s)")

PEDIDO_RESPONSE=$(curl -s -X POST http://localhost:5000/api/pedidos/checkout \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "idempotencyKey": "'$IDEMPOTENCY_KEY'"
  }')

echo "Resposta do checkout:"
echo $PEDIDO_RESPONSE | python3 -m json.tool 2>/dev/null || echo $PEDIDO_RESPONSE

# Verificar se houve erro
if echo "$PEDIDO_RESPONSE" | grep -q "null value in column"; then
    echo -e "\n❌ ERRO: Ainda há problema com pedido_id NULL!"
    exit 1
elif echo "$PEDIDO_RESPONSE" | grep -q '"id"'; then
    echo -e "\n✅ SUCESSO: Pedido criado sem erro de pedido_id NULL!"
    
    # Pegar o ID do pedido criado
    PEDIDO_ID=$(echo $PEDIDO_RESPONSE | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
    echo "ID do pedido criado: $PEDIDO_ID"
    
    # 6. Verificar detalhes do pedido
    echo -e "\n6. Verificando detalhes do pedido..."
    PEDIDO_DETALHES=$(curl -s -X GET http://localhost:5000/api/pedidos/$PEDIDO_ID \
      -H "Authorization: Bearer $TOKEN")
    
    echo "Detalhes do pedido:"
    echo $PEDIDO_DETALHES | python3 -m json.tool 2>/dev/null || echo $PEDIDO_DETALHES
    
    # 7. Listar meus pedidos
    echo -e "\n7. Listando meus pedidos..."
    MEUS_PEDIDOS=$(curl -s -X GET http://localhost:5000/api/pedidos/meus \
      -H "Authorization: Bearer $TOKEN")
    
    echo "Meus pedidos:"
    echo $MEUS_PEDIDOS | python3 -m json.tool 2>/dev/null || echo $MEUS_PEDIDOS
    
    exit 0
else
    echo -e "\n❓ Resposta inesperada do servidor"
    exit 1
fi