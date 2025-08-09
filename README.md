
# E-commerce Pedidos RUP

Sistema de gerenciamento de pedidos para e-commerce desenvolvido seguindo os princípios do RUP (Rational Unified Process).

## Estrutura do Projeto

```
ecommerce-pedidos-rup/
├── README.md
├── app-cli/
│   └── src/main/java/
│       ├── app/
│       │   └── Main.java
│       ├── domain/
│       │   ├── Categoria.java
│       │   ├── Produto.java
│       │   ├── Cliente.java
│       │   ├── Endereco.java
│       │   ├── Pedido.java
│       │   ├── ItemPedido.java
│       │   ├── StatusPedido.java
│       │   ├── Pagamento.java
│       │   ├── PagamentoCartao.java
│       │   ├── PagamentoBoleto.java
│       │   └── PagamentoPix.java
│       ├── infra/
│       │   ├── CsvUtil.java
│       │   ├── RepositorioProdutosCsv.java
│       │   └── RepositorioPedidosCsv.java
│       └── service/
│           └── PedidoService.java
├── data/
│   ├── produtos.csv
│   ├── clientes.csv
│   ├── enderecos.csv
│   ├── pedidos.csv
│   └── itens_pedido.csv
└── pom.xml
```

## Funcionalidades

- Gerenciamento de produtos
- Criação e gerenciamento de pedidos
- Diferentes formas de pagamento (Cartão, Boleto, PIX)
- Controle de estoque
- Persistência em arquivos CSV

## Como Executar

1. Compile o projeto:
   ```bash
   javac -classpath .:target/dependency/* -d . $(find . -type f -name '*.java')
   ```

2. Execute a aplicação:
   ```bash
   java -classpath .:target/dependency/* app.Main
   ```

## Arquitetura

O sistema segue uma arquitetura em camadas:

- **app**: Camada de apresentação (interface CLI)
- **domain**: Entidades de domínio
- **service**: Lógica de negócio
- **infra**: Infraestrutura e persistência

## Dados de Exemplo

O sistema vem com dados de exemplo pré-carregados nos arquivos CSV:
- 5 produtos de diferentes categorias
- 3 clientes com endereços
- Estrutura para pedidos e itens de pedido
