// Tipos de autenticação
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  role: 'CUSTOMER' | 'MANAGER';
}

export interface LoginResponse {
  token: string;
  email: string;
  role: 'CUSTOMER' | 'MANAGER' | 'COURIER';
}

export interface User {
  id: string;
  email: string;
  role: 'CUSTOMER' | 'MANAGER' | 'COURIER';
  isActive: boolean;
  createdAt: string;
  lastLoginAt?: string;
}

// Tipos de produto e categoria
export interface Categoria {
  id: string;
  nome: string;
}

export interface Produto {
  id: string;
  nome: string;
  preco: number;
  codigoBarras: string;
  categoriaId: string;
  categoria?: Categoria;
}

// Tipos de cliente e endereço
export interface Cliente {
  id: string;
  nome: string;
  email: string;
}

export interface Endereco {
  id: string;
  rua: string;
  numero: string;
  cidade: string;
  cep: string;
  clienteId: string;
}

// Tipos de carrinho
export interface ItemCarrinho {
  id: string;
  quantidade: number;
  produtoId: string;
  produto?: Produto;
  carrinhoId: string;
}

export interface Carrinho {
  id: string;
  clienteId: string;
  itens: ItemCarrinho[];
}

// Tipos de pedido
export type StatusPedido = 'PROCESSANDO' | 'PAGO' | 'ENVIADO' | 'ENTREGUE' | 'CANCELADO';

export interface ItemPedido {
  id: string;
  quantidade: number;
  precoUnitario: number;
  subtotal: number;
  produtoId: string;
  produto?: Produto;
}

export interface Pedido {
  id: string;
  valorTotal: number;
  dataPedido: string;
  status: StatusPedido;
  clienteId: string;
  cliente?: Cliente;
  enderecoId: string;
  endereco?: Endereco;
  itens: ItemPedido[];
}

// Tipos de pagamento
export type TipoPagamento = 'PIX' | 'CARTAO' | 'BOLETO';

export interface Pagamento {
  id: string;
  valor: number;
  pedidoId: string;
  tipo?: TipoPagamento;
}

export interface PagamentoPix extends Pagamento {
  txid: string;
  qrCode?: string;
  paymentUrl?: string;
}

export interface PagamentoCartao extends Pagamento {
  tokenCartao: string;
  bandeira: string;
}

export interface PagamentoBoleto extends Pagamento {
  linhaDigitavel: string;
  linkBoleto?: string;
}

// Tipos de notificação
export type TipoNotificacao = 'CONFIRMACAO' | 'STATUS';

export interface Notificacao {
  id: string;
  mensagem: string;
  tipo: TipoNotificacao;
  criadoEm: string;
  clienteId: string;
  pedidoId: string;
}

// Tipos de request para criar/atualizar
export interface CreateCategoriaRequest {
  nome: string;
}

export interface CreateProdutoRequest {
  nome: string;
  preco: number;
  codigoBarras: string;
  categoriaId: string;
}

export interface CreateClienteRequest {
  nome: string;
  email: string;
}

export interface CreateEnderecoRequest {
  rua: string;
  numero: string;
  cidade: string;
  cep: string;
}

export interface AddItemCarrinhoRequest {
  produtoId: string;
  quantidade: number;
}

export interface UpdateItemCarrinhoRequest {
  quantidade: number;
}

export interface CreatePedidoRequest {
  enderecoId: string;
}

export interface CreatePagamentoPixRequest {
  valor: number;
}

export interface CreatePagamentoCartaoRequest {
  valor: number;
  tokenCartao: string;
  bandeira: string;
}

export interface CreatePagamentoBoletoRequest {
  valor: number;
}

// Tipos de resposta de API
export interface ApiResponse<T> {
  data: T;
  message?: string;
}

export interface ApiError {
  message: string;
  code?: string;
  details?: string;
}

export interface PaginatedResponse<T> {
  data: T[];
  total: number;
  page: number;
  limit: number;
}