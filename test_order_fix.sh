#!/bin/bash

# Test script to verify order confirmation fix

echo "🧪 Testing Order Confirmation Fix..."

# Login to get token
echo "1. Logging in..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:5000/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }')

TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*' | sed 's/"token":"//')

if [ -z "$TOKEN" ]; then
  echo "❌ Login failed. Creating test user..."
  
  # Register if login fails
  REGISTER_RESPONSE=$(curl -s -X POST http://localhost:5000/auth/register \
    -H "Content-Type: application/json" \
    -d '{
      "email": "test@example.com",
      "password": "password123"
    }')
  
  TOKEN=$(echo $REGISTER_RESPONSE | grep -o '"token":"[^"]*' | sed 's/"token":"//')
fi

echo "✅ Authenticated with token: ${TOKEN:0:20}..."

# Add an address
echo "2. Adding address..."
ADDRESS_RESPONSE=$(curl -s -X POST http://localhost:5000/api/enderecos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "cep": "12345678",
    "logradouro": "Test Street",
    "numero": "123",
    "complemento": "Apt 1",
    "bairro": "Test District",
    "cidade": "Test City",
    "uf": "TS",
    "tipo": "ENTREGA"
  }')

ADDRESS_ID=$(echo $ADDRESS_RESPONSE | grep -o '"id":"[^"]*' | sed 's/"id":"//')
echo "✅ Address created: $ADDRESS_ID"

# Add items to cart
echo "3. Adding items to cart..."
CART_RESPONSE=$(curl -s -X POST http://localhost:5000/api/carrinho/add \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "produtoId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
    "quantidade": 2
  }')

# Confirm order
echo "4. Confirming order..."
ORDER_RESPONSE=$(curl -s -X POST http://localhost:5000/api/pedidos/confirmar \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-$(date +%s)" \
  -d "{
    \"enderecoId\": \"$ADDRESS_ID\",
    \"pagamento\": {
      \"metodo\": \"CARTAO\",
      \"tokenCartao\": \"tok_test_123456\",
      \"bandeira\": \"VISA\",
      \"ultimosDigitos\": \"4242\"
    }
  }")

echo "Order Response:"
echo "$ORDER_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$ORDER_RESPONSE"

# Check for error
if echo "$ORDER_RESPONSE" | grep -q "Usuário não encontrado"; then
  echo "❌ ERROR: User not found error still occurs!"
  exit 1
elif echo "$ORDER_RESPONSE" | grep -q '"id"'; then
  ORDER_ID=$(echo $ORDER_RESPONSE | grep -o '"id":"[^"]*' | head -1 | sed 's/"id":"//')
  echo "✅ Order created successfully: $ORDER_ID"
  echo "✨ Fix verified! Orders can now be confirmed without 'User not found' error."
  exit 0
else
  echo "⚠️ Unexpected response. Please check manually."
  exit 1
fi