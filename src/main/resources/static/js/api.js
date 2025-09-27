// API Client for E-commerce application

// Define Auth globally if not already defined
if (typeof Auth === 'undefined') {
    window.Auth = {
        get token() { 
            return localStorage.getItem('token') || ''; 
        },
        set token(v) { 
            if (v) localStorage.setItem('token', v); 
            else localStorage.removeItem('token'); 
        },
        get user() { 
            try { 
                return JSON.parse(localStorage.getItem('user') || '{}'); 
            } catch { 
                return null; 
            } 
        },
        set user(u) { 
            if (u) localStorage.setItem('user', JSON.stringify(u)); 
            else localStorage.removeItem('user'); 
        },
        clear() { 
            localStorage.removeItem('token');
            localStorage.removeItem('user');
        }
    };
}

// Auth headers helper
function authHeaders() {
    const t = Auth.token;
    const h = { 'Content-Type': 'application/json' };
    if (t) h['Authorization'] = 'Bearer ' + t;
    return h;
}

// Fetch wrapper with auth handling
async function apiFetch(url, options = {}) {
    const config = { 
        cache: 'no-store', 
        ...options, 
        headers: { ...(options.headers || {}), ...authHeaders() } 
    };
    
    const response = await fetch(url, config);
    
    if (response.status === 401) {
        if (url.includes('/api/auth/login') || url.includes('/api/auth/register')) return response;
        
        const retry = await fetch(url + (url.includes('?') ? '&' : '?') + 'retry=' + Date.now(), config);
        
        if (retry.status === 401) { 
            Auth.clear(); 
            window.location.href = '/'; 
            return retry; 
        }
        return retry;
    }
    return response;
}

class ApiClient {
    constructor() {
        this.baseURL = '';
        this.token = null;
        this.loadToken();
    }

    loadToken() {
        this.token = Auth.token;
    }

    setToken(token) {
        this.token = token;
        Auth.token = token;
    }

    getHeaders() {
        return authHeaders();
    }

    async request(endpoint, options = {}) {
        const url = `${this.baseURL}${endpoint}`;
        
        try {
            const response = await apiFetch(url, options);
            
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || `Erro HTTP: ${response.status}`);
            }

            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                return await response.json();
            }
            
            return null;
        } catch (error) {
            console.error('API Error:', error);
            throw error;
        }
    }

    handleUnauthorized() {
        Auth.clear();
        window.dispatchEvent(new CustomEvent('unauthorized'));
    }

    // Auth endpoints
    async login(email, password) {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password }),
            cache: 'no-store'
        });
        
        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || 'Login falhou');
        }
        
        const data = await response.json();
        
        if (data && data.token) {
            Auth.token = data.token;
            this.setToken(data.token);
            if (data.user) {
                Auth.user = data.user;
            }
        }
        
        return data;
    }

    async register(email, password, role) {
        const response = await apiFetch('/api/auth/register', {
            method: 'POST',
            body: JSON.stringify({ email, password, role })
        });
        
        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || 'Registro falhou');
        }
        
        return response.json();
    }

    async getProfile() {
        const response = await apiFetch('/api/auth/profile');
        if (!response.ok) throw new Error('Erro ao obter perfil');
        return response.json();
    }
    
    async getMe() {
        const response = await apiFetch(`/api/auth/me?ts=${Date.now()}`);
        if (!response.ok) throw new Error('Não autenticado');
        return response.json();
    }

    // Categories endpoints
    async getCategorias() {
        const response = await apiFetch(`/api/categorias?ts=${Date.now()}`);
        if (!response.ok) throw new Error('Erro ao carregar categorias');
        return response.json();
    }

    async createCategoria(nome) {
        const response = await apiFetch('/api/categorias', {
            method: 'POST',
            body: JSON.stringify({ nome })
        });
        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || 'Erro ao criar categoria');
        }
        return response.json();
    }

    async updateCategoria(id, nome) {
        const response = await apiFetch(`/api/categorias/${id}`, {
            method: 'PUT',
            body: JSON.stringify({ nome })
        });
        if (!response.ok) throw new Error('Erro ao atualizar categoria');
        return response.json();
    }

    async deleteCategoria(id) {
        const response = await apiFetch(`/api/categorias/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) throw new Error('Erro ao deletar categoria');
        return null;
    }

    // Products endpoints
    async getProdutos() {
        const response = await apiFetch(`/api/produtos?ts=${Date.now()}`);
        if (!response.ok) throw new Error('Erro ao carregar produtos');
        return response.json();
    }

    async createProduto(produto) {
        const response = await apiFetch('/api/produtos', {
            method: 'POST',
            body: JSON.stringify(produto)
        });
        if (!response.ok) throw new Error('Erro ao criar produto');
        return response.json();
    }

    async updateProduto(id, produto) {
        const response = await apiFetch(`/api/produtos/${id}`, {
            method: 'PUT',
            body: JSON.stringify(produto)
        });
        if (!response.ok) throw new Error('Erro ao atualizar produto');
        return response.json();
    }

    async deleteProduto(id) {
        const response = await apiFetch(`/api/produtos/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) throw new Error('Erro ao deletar produto');
        return null;
    }

    // Clients endpoints
    async getClientes() {
        const response = await apiFetch('/api/clientes');
        if (!response.ok) throw new Error('Erro ao carregar clientes');
        return response.json();
    }

    async createCliente(cliente) {
        const response = await apiFetch('/api/clientes', {
            method: 'POST',
            body: JSON.stringify(cliente)
        });
        if (!response.ok) throw new Error('Erro ao criar cliente');
        return response.json();
    }

    // Orders endpoints
    async getPedidos() {
        const response = await apiFetch('/api/pedidos');
        if (!response.ok) throw new Error('Erro ao carregar pedidos');
        return response.json();
    }

    async getPedidosByCliente(clienteId) {
        const response = await apiFetch(`/api/pedidos/cliente/${clienteId}`);
        if (!response.ok) throw new Error('Erro ao carregar pedidos do cliente');
        return response.json();
    }

    async updatePedidoStatus(id, status) {
        const response = await apiFetch(`/api/admin/pedidos/${id}/status`, {
            method: 'PATCH',
            body: JSON.stringify({ status })
        });
        
        if (!response.ok) {
            if (response.status === 400) {
                const error = await response.json();
                throw new Error(error.message || 'Transição de status inválida');
            } else if (response.status === 404) {
                throw new Error('Pedido não encontrado');
            } else if (response.status === 409) {
                throw new Error('Conflito de versão ao atualizar');
            } else {
                throw new Error('Erro ao atualizar status do pedido');
            }
        }
        
        return response.json();
    }

    // Payments endpoints
    async getPagamentos() {
        const response = await apiFetch('/api/pagamentos');
        if (!response.ok) throw new Error('Erro ao carregar pagamentos');
        return response.json();
    }

    async getPagamentoTotal() {
        const response = await apiFetch('/api/pagamentos/total');
        if (!response.ok) throw new Error('Erro ao obter total de pagamentos');
        return response.json();
    }

    // Address endpoints
    async getEnderecos() {
        const response = await apiFetch(`/api/enderecos?ts=${Date.now()}`);
        if (!response.ok) throw new Error('Erro ao carregar endereços');
        return response.json();
    }

    async getEndereco(id) {
        const response = await apiFetch(`/api/enderecos/${id}`);
        if (!response.ok) throw new Error('Erro ao carregar endereço');
        return response.json();
    }

    async createEndereco(dto) {
        const response = await apiFetch('/api/enderecos', {
            method: 'POST',
            body: JSON.stringify(dto)
        });
        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || 'Erro ao criar endereço');
        }
        return response.json();
    }

    async updateEndereco(id, dto) {
        const response = await apiFetch(`/api/enderecos/${id}`, {
            method: 'PATCH',
            body: JSON.stringify(dto)
        });
        if (!response.ok) throw new Error('Erro ao atualizar endereço');
        return response.json();
    }

    async deleteEndereco(id) {
        const response = await apiFetch(`/api/enderecos/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) throw new Error('Erro ao deletar endereço');
        return null;
    }

    // Payment simulation
    async simularPagamento(payload) {
        const response = await apiFetch('/api/pagamentos/simular', {
            method: 'POST',
            body: JSON.stringify(payload)
        });
        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || 'Erro ao simular pagamento');
        }
        return response.json();
    }

    // Stats endpoints
    async getStats() {
        const [produtos, pedidos, clientes, faturamento] = await Promise.all([
            apiFetch('/api/produtos').then(r => r.ok ? r.json() : []).then(data => data?.length || 0).catch(() => 0),
            apiFetch('/api/pedidos').then(r => r.ok ? r.json() : []).then(data => data?.length || 0).catch(() => 0),
            apiFetch('/api/clientes').then(r => r.ok ? r.json() : []).then(data => data?.length || 0).catch(() => 0),
            apiFetch('/api/pagamentos/total').then(r => r.ok ? r.json() : {}).then(data => data?.total || 0).catch(() => 0)
        ]);

        return {
            totalProdutos: produtos,
            totalPedidos: pedidos,
            totalClientes: clientes,
            totalFaturamento: faturamento
        };
    }
    
    async getMetricasAdmin() {
        const response = await apiFetch(`/api/admin/metricas?ts=${Date.now()}`);
        if (!response.ok) throw new Error('Erro ao carregar métricas');
        return response.json();
    }

    // Cart endpoints
    async getCarrinho(clienteId) {
        const url = clienteId 
            ? `/api/carrinho/${clienteId}?ts=${Date.now()}`
            : `/api/carrinho?ts=${Date.now()}`;
        const response = await apiFetch(url);
        if (!response.ok) throw new Error('Erro ao carregar carrinho');
        return response.json();
    }

    async addToCarrinho(clienteId, produtoId, quantidade = 1) {
        const url = clienteId 
            ? `/api/carrinho/${clienteId}/itens`
            : '/api/carrinho/item';
        const response = await apiFetch(url, {
            method: 'POST',
            body: JSON.stringify({ produtoId, quantidade })
        });
        if (!response.ok) throw new Error('Erro ao adicionar item ao carrinho');
        return response.json();
    }

    async addItemCarrinho(produtoId, quantidade = 1) {
        const response = await apiFetch('/api/carrinho/item', {
            method: 'POST',
            body: JSON.stringify({ produtoId, quantidade })
        });
        if (!response.ok) throw new Error('Erro ao adicionar item ao carrinho');
        return response.json();
    }
    
    async removeItemCarrinho(produtoId) {
        const response = await apiFetch(`/api/carrinho/item/${produtoId}`, {
            method: 'DELETE'
        });
        if (!response.ok) throw new Error('Erro ao remover item do carrinho');
        return response.json();
    }

    async updateCarrinhoItem(clienteId, itemId, quantidade) {
        const response = await apiFetch(`/api/carrinho/${clienteId}/itens/${itemId}`, {
            method: 'PUT',
            body: JSON.stringify({ quantidade })
        });
        if (!response.ok) throw new Error('Erro ao atualizar item do carrinho');
        return response.json();
    }

    async removeCarrinhoItem(clienteId, itemId) {
        const response = await apiFetch(`/api/carrinho/${clienteId}/itens/${itemId}`, {
            method: 'DELETE'
        });
        if (!response.ok) throw new Error('Erro ao remover item do carrinho');
        return null;
    }

    async clearCarrinho(clienteId) {
        const url = clienteId 
            ? `/api/carrinho/${clienteId}`
            : '/api/carrinho';
        const response = await apiFetch(url, {
            method: 'DELETE'
        });
        if (!response.ok) throw new Error('Erro ao limpar carrinho');
        return null;
    }

    async getCarrinhoTotal(clienteId) {
        const response = await apiFetch(`/api/carrinho/${clienteId}/total`);
        if (!response.ok) throw new Error('Erro ao obter total do carrinho');
        return response.json();
    }

    async getCarrinhoCount(clienteId) {
        const response = await apiFetch(`/api/carrinho/${clienteId}/count`);
        if (!response.ok) throw new Error('Erro ao obter contagem do carrinho');
        return response.json();
    }

    // Order confirmation with Idempotency-Key
    async confirmarPedido(payload) {
        const idempotencyKey = crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
        const response = await apiFetch('/api/pedidos/confirmar', {
            method: 'POST',
            headers: {
                'Idempotency-Key': idempotencyKey
            },
            body: JSON.stringify(payload)
        });
        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || 'Erro ao confirmar pedido');
        }
        return response.json();
    }
    
    // Order methods for checkout
    generateUUID() {
        if (typeof crypto !== 'undefined' && crypto.randomUUID) {
            return crypto.randomUUID();
        }
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            const r = Math.random() * 16 | 0;
            const v = c === 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    }
    
    async criarPedido() {
        const idempotencyKey = this.generateUUID();
        const response = await apiFetch('/api/pedidos', {
            method: 'POST',
            headers: { 
                'Idempotency-Key': idempotencyKey 
            },
            body: JSON.stringify({ origem: 'carrinho' })
        });
        
        if (response.ok) {
            const data = await response.json();
            return { success: true, data };
        } else if (response.status === 400) {
            const error = await response.text();
            return { success: false, error: error || 'Carrinho vazio' };
        } else {
            throw new Error('Erro ao criar pedido');
        }
    }
    
    async getMeusPedidos() {
        const response = await apiFetch(`/api/pedidos/me?ts=${Date.now()}`);
        if (!response.ok) return [];
        return response.json();
    }
    
    async getPedidosAdmin() {
        const response = await apiFetch(`/api/admin/pedidos?ts=${Date.now()}`);
        if (!response.ok) return [];
        return response.json();
    }

    // User management
    async getCurrentUser() {
        try {
            if (!Auth.token) return null;
            const profile = await this.getMe();
            return profile;
        } catch (error) {
            console.error('Error getting current user:', error);
            return null;
        }
    }

    // Health check
    async healthCheck() {
        const response = await fetch('/health', {
            cache: 'no-store'
        });
        if (!response.ok) throw new Error('API não está respondendo');
        return response.json();
    }
    
    logout() {
        Auth.clear();
    }
}

// Global API client instance
window.api = new ApiClient();