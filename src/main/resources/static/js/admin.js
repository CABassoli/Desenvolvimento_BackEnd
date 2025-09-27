function fmtIso(s) {
    if (!s) return '-';
    const d = new Date(s);
    return isNaN(d) ? '-' : d.toLocaleDateString('pt-BR') + ' ' + d.toLocaleTimeString('pt-BR', {hour: '2-digit', minute: '2-digit'});
}

// Mantém o nome antigo por compatibilidade
function fmtDataIso(s) {
    return fmtIso(s);
}

class AdminManager {
    constructor() {
        this.currentTab = 'categorias';
        this.categorias = [];
        this.produtos = [];
        this.pedidos = [];
        this.clientes = [];
        this.pedidosRefreshInterval = null;
        this.metricsRefreshInterval = null;
        this.setupEventListeners();
        this.startAutoRefresh();
    }

    setupEventListeners() {
        // Tab switching
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const tab = e.target.getAttribute('data-tab');
                this.switchTab(tab);
            });
        });

        // Add buttons
        document.getElementById('add-categoria-btn').addEventListener('click', () => {
            this.showAddCategoriaModal();
        });

        document.getElementById('add-produto-btn').addEventListener('click', () => {
            this.showAddProdutoModal();
        });

        window.addEventListener('focus', () => {
            if (this.currentTab === 'pedidos') {
                this.refreshPedidos();
            }
        });
    }

    async loadDashboard() {
        await this.loadStats();
        await this.loadData();
        this.renderCurrentTab();
    }
    
    startAutoRefresh() {
        if (this.metricsRefreshInterval) {
            clearInterval(this.metricsRefreshInterval);
        }
        
        if (this.pedidosRefreshInterval) {
            clearInterval(this.pedidosRefreshInterval);
        }
        
        this.metricsRefreshInterval = setInterval(async () => {
            await this.loadStats();
        }, 15000);
        
        this.pedidosRefreshInterval = setInterval(async () => {
            if (this.currentTab === 'pedidos') {
                await this.refreshPedidos();
            }
        }, 15000);
    }
    
    async refreshPedidos() {
        try {
            this.pedidos = await window.api.getPedidosAdmin();
            this.renderPedidos();
        } catch (error) {
            console.error('Error refreshing pedidos:', error);
        }
    }

    async loadStats() {
        try {
            // Forçar cache bust com timestamp
            const response = await apiFetch(`/api/admin/metricas?ts=${Date.now()}`);
            if (!response.ok) throw new Error('Erro ao carregar métricas');
            const stats = await response.json();
            
            document.getElementById('total-produtos').textContent = stats.totalProdutos || 0;
            document.getElementById('total-pedidos').textContent = stats.totalPedidos || 0;
            document.getElementById('total-clientes').textContent = stats.totalClientes || 0;
            document.getElementById('total-faturamento').textContent = 
                new Intl.NumberFormat('pt-BR', { 
                    style: 'currency', 
                    currency: 'BRL' 
                }).format(stats.faturamento || 0);
        } catch (error) {
            console.error('Error loading stats:', error);
        }
    }

    async loadData() {
        try {
            // Carregar dados sequencialmente para evitar sobrecarga de conexões
            try {
                this.categorias = await window.api.getCategorias();
            } catch (e) {
                console.error('Erro ao carregar categorias:', e);
                this.categorias = [];
            }
            
            try {
                this.produtos = await window.api.getProdutos();
            } catch (e) {
                console.error('Erro ao carregar produtos:', e);
                this.produtos = [];
            }
            
            try {
                this.pedidos = await window.api.getPedidosAdmin(false);
            } catch (e) {
                console.error('Erro ao carregar pedidos:', e);
                this.pedidos = [];
            }
            
            try {
                this.clientes = await window.api.getClientes();
            } catch (e) {
                console.error('Erro ao carregar clientes:', e);
                this.clientes = [];
            }
        } catch (error) {
            console.error('Error loading data:', error);
        }
    }

    switchTab(tab) {
        // Update tab buttons
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.classList.remove('border-blue-500', 'text-blue-600');
            btn.classList.add('border-transparent', 'text-gray-500');
        });
        
        document.querySelector(`[data-tab="${tab}"]`).classList.remove('border-transparent', 'text-gray-500');
        document.querySelector(`[data-tab="${tab}"]`).classList.add('border-blue-500', 'text-blue-600');

        // Hide all tab contents
        document.querySelectorAll('.tab-content').forEach(content => {
            content.classList.add('hidden');
        });

        // Show selected tab
        document.getElementById(`tab-${tab}`).classList.remove('hidden');
        
        this.currentTab = tab;
        this.renderCurrentTab();
        
        this.stopPedidosAutoRefresh();
        
        if (tab === 'pedidos') {
            this.refreshPedidos();
            this.startPedidosAutoRefresh();
        }
    }

    renderCurrentTab() {
        switch (this.currentTab) {
            case 'categorias':
                this.renderCategorias();
                break;
            case 'produtos':
                this.renderProdutos();
                break;
            case 'pedidos':
                this.renderPedidos();
                break;
            case 'clientes':
                this.renderClientes();
                break;
        }
    }

    renderCategorias() {
        const container = document.getElementById('categorias-list');
        
        if (this.categorias.length === 0) {
            container.innerHTML = '<p class="text-gray-500">Nenhuma categoria encontrada.</p>';
            return;
        }

        container.innerHTML = this.categorias.map(categoria => `
            <div class="list-item">
                <div>
                    <h4 class="font-semibold">${categoria.nome}</h4>
                    <p class="text-sm text-gray-500">ID: ${categoria.id}</p>
                </div>
                <div class="space-x-2">
                    <button onclick="window.adminManager.editCategoria('${categoria.id}')" 
                            class="text-blue-600 hover:text-blue-800">Editar</button>
                    <button onclick="window.adminManager.deleteCategoria('${categoria.id}')" 
                            class="text-red-600 hover:text-red-800">Excluir</button>
                </div>
            </div>
        `).join('');
    }

    renderProdutos() {
        const container = document.getElementById('produtos-list');
        
        if (this.produtos.length === 0) {
            container.innerHTML = '<p class="text-gray-500">Nenhum produto encontrado.</p>';
            return;
        }

        container.innerHTML = this.produtos.map(produto => {
            const categoria = this.categorias.find(c => c.id === produto.categoriaId);
            const preco = new Intl.NumberFormat('pt-BR', { 
                style: 'currency', 
                currency: 'BRL' 
            }).format(produto.preco);

            return `
                <div class="list-item">
                    <div>
                        <h4 class="font-semibold">${produto.nome}</h4>
                        <p class="text-sm text-gray-500">${preco} - ${categoria?.nome || 'Sem categoria'}</p>
                        <p class="text-xs text-gray-400">Código: ${produto.codigoBarras}</p>
                    </div>
                    <div class="space-x-2">
                        <button onclick="window.adminManager.editProduto('${produto.id}')" 
                                class="text-blue-600 hover:text-blue-800">Editar</button>
                        <button onclick="window.adminManager.deleteProduto('${produto.id}')" 
                                class="text-red-600 hover:text-red-800">Excluir</button>
                    </div>
                </div>
            `;
        }).join('');
    }

    async updatePedidoStatus(pedidoId, novoStatus) {
        const selectElement = document.querySelector(`select[data-pedido-id="${pedidoId}"]`);
        const oldStatus = selectElement ? selectElement.getAttribute('data-original-value') : null;
        
        if (selectElement) {
            selectElement.disabled = true;
        }
        
        try {
            const updatedPedido = await window.api.updatePedidoStatus(pedidoId, novoStatus);
            
            // Atualizar o pedido na lista local
            const pedidoIndex = this.pedidos.findIndex(p => p.id === pedidoId);
            if (pedidoIndex >= 0 && updatedPedido) {
                this.pedidos[pedidoIndex] = updatedPedido;
            }
            
            // Forçar atualização das métricas com cache bust
            await this.loadStats();
            
            // Re-renderizar a lista de pedidos
            this.renderPedidos();
            
            this.showToast('Status atualizado com sucesso', 'success');
        } catch (error) {
            // Restaurar o valor original em caso de erro
            if (selectElement && oldStatus) {
                selectElement.value = oldStatus;
            }
            
            this.showToast(error.message || 'Erro ao atualizar status', 'error');
            
            // Recarregar pedidos para garantir sincronização
            await this.refreshPedidos();
        } finally {
            if (selectElement) {
                selectElement.disabled = false;
            }
        }
    }

    renderPedidos() {
        const container = document.getElementById('pedidos-list');
        
        if (this.pedidos.length === 0) {
            container.innerHTML = '<p class="text-gray-500">Nenhum pedido encontrado.</p>';
            return;
        }

        const pedidosOrdenados = [...this.pedidos].sort((a, b) => {
            const dataA = new Date(a.createdAt || a.dataPedido);
            const dataB = new Date(b.createdAt || b.dataPedido);
            return dataB - dataA;
        });

        container.innerHTML = pedidosOrdenados.map(pedido => {
            const valor = new Intl.NumberFormat('pt-BR', { 
                style: 'currency', 
                currency: 'BRL' 
            }).format(pedido.valorTotal || pedido.total || 0);

            // Formatar datas usando fmtIso
            const dataCriacao = fmtIso(pedido.createdAt || pedido.dataPedido);
            const dataAtualizacao = pedido.updatedAt ? `Atualizado: ${fmtIso(pedido.updatedAt)}` : '';
            const dataPago = pedido.paidAt ? `Pago: ${fmtIso(pedido.paidAt)}` : '';
            const dataCancelado = pedido.canceledAt ? `Cancelado: ${fmtIso(pedido.canceledAt)}` : '';

            // Gerar opções do select baseado no status atual
            const statusOptions = this.getStatusOptions(pedido.status);

            return `
                <div class="list-item">
                    <div>
                        <h4 class="font-semibold">Pedido #${pedido.id.substring(0, 8)}</h4>
                        <p class="text-sm text-gray-600">${valor} - ${dataCriacao}</p>
                        ${dataAtualizacao ? `<p class="text-xs text-gray-500">${dataAtualizacao}</p>` : ''}
                        ${dataPago ? `<p class="text-xs text-green-600">${dataPago}</p>` : ''}
                        ${dataCancelado ? `<p class="text-xs text-red-600">${dataCancelado}</p>` : ''}
                        <span class="status-${pedido.status.toLowerCase()}">${pedido.status}</span>
                    </div>
                    <div class="space-x-2">
                        <select data-pedido-id="${pedido.id}" 
                                data-original-value="${pedido.status}"
                                onchange="window.adminManager.updatePedidoStatus('${pedido.id}', this.value)"
                                class="text-sm border rounded px-2 py-1"
                                ${pedido.status === 'ENTREGUE' || pedido.status === 'CANCELADO' ? 'disabled' : ''}>
                            ${statusOptions}
                        </select>
                    </div>
                </div>
            `;
        }).join('');
        
        // Após renderizar, garantir que os selects tenham o valor correto
        pedidosOrdenados.forEach(pedido => {
            const select = document.querySelector(`select[data-pedido-id="${pedido.id}"]`);
            if (select) {
                select.value = pedido.status;
            }
        });
    }
    
    getStatusOptions(currentStatus) {
        const allStatuses = ['NOVO', 'PROCESSANDO', 'PAGO', 'ENVIADO', 'ENTREGUE', 'CANCELADO'];
        let availableStatuses = [];
        
        switch(currentStatus) {
            case 'NOVO':
                availableStatuses = ['NOVO', 'PROCESSANDO', 'CANCELADO'];
                break;
            case 'PROCESSANDO':
                availableStatuses = ['PROCESSANDO', 'PAGO', 'CANCELADO'];
                break;
            case 'PAGO':
                availableStatuses = ['PAGO', 'ENVIADO', 'CANCELADO'];
                break;
            case 'ENVIADO':
                availableStatuses = ['ENVIADO', 'ENTREGUE', 'CANCELADO'];
                break;
            case 'ENTREGUE':
                availableStatuses = ['ENTREGUE'];
                break;
            case 'CANCELADO':
                availableStatuses = ['CANCELADO'];
                break;
            default:
                availableStatuses = [currentStatus];
        }
        
        return availableStatuses.map(status => 
            `<option value="${status}" ${status === currentStatus ? 'selected' : ''}>${status}</option>`
        ).join('');
    }

    renderClientes() {
        const container = document.getElementById('clientes-list');
        
        if (this.clientes.length === 0) {
            container.innerHTML = '<p class="text-gray-500">Nenhum cliente encontrado.</p>';
            return;
        }

        container.innerHTML = this.clientes.map(cliente => `
            <div class="list-item">
                <div>
                    <h4 class="font-semibold">${cliente.nome}</h4>
                    <p class="text-sm text-gray-500">${cliente.email}</p>
                </div>
            </div>
        `).join('');
    }

    // Modal methods
    showAddCategoriaModal() {
        this.showModal(`
            <div class="modal-header">
                <h3 class="modal-title">Adicionar Categoria</h3>
                <span class="modal-close" onclick="window.adminManager.closeModal()">&times;</span>
            </div>
            <form onsubmit="window.adminManager.submitCategoria(event)">
                <div class="mb-4">
                    <label class="block text-sm font-medium mb-2">Nome da Categoria:</label>
                    <input type="text" id="categoria-nome" required class="form-input">
                </div>
                <div class="flex justify-end space-x-2">
                    <button type="button" onclick="window.adminManager.closeModal()" 
                            class="btn-secondary">Cancelar</button>
                    <button type="submit" class="btn-primary">Adicionar</button>
                </div>
            </form>
        `);
    }

    showAddProdutoModal() {
        const categoriasOptions = this.categorias.map(c => 
            `<option value="${c.id}">${c.nome}</option>`
        ).join('');

        this.showModal(`
            <div class="modal-header">
                <h3 class="modal-title">Adicionar Produto</h3>
                <span class="modal-close" onclick="window.adminManager.closeModal()">&times;</span>
            </div>
            <form onsubmit="window.adminManager.submitProduto(event)">
                <div class="mb-4">
                    <label class="block text-sm font-medium mb-2">Nome do Produto:</label>
                    <input type="text" id="produto-nome" required class="form-input">
                </div>
                <div class="mb-4">
                    <label class="block text-sm font-medium mb-2">Preço:</label>
                    <input type="number" step="0.01" id="produto-preco" required class="form-input">
                </div>
                <div class="mb-4">
                    <label class="block text-sm font-medium mb-2">Código de Barras:</label>
                    <input type="text" id="produto-codigo" required class="form-input">
                </div>
                <div class="mb-4">
                    <label class="block text-sm font-medium mb-2">Categoria:</label>
                    <select id="produto-categoria" required class="form-select">
                        <option value="">Selecione uma categoria</option>
                        ${categoriasOptions}
                    </select>
                </div>
                <div class="flex justify-end space-x-2">
                    <button type="button" onclick="window.adminManager.closeModal()" 
                            class="btn-secondary">Cancelar</button>
                    <button type="submit" class="btn-primary">Adicionar</button>
                </div>
            </form>
        `);
    }

    showModal(content) {
        document.getElementById('modal-content').innerHTML = content;
        document.getElementById('modal-overlay').classList.remove('hidden');
    }

    closeModal() {
        document.getElementById('modal-overlay').classList.add('hidden');
    }

    showToast(message, type = 'success') {
        if (type === 'success') {
            window.showSuccess(message);
        } else {
            window.showError(message);
        }
    }

    // Submit methods
    async submitCategoria(event) {
        event.preventDefault();
        const nome = document.getElementById('categoria-nome').value.trim();

        if (!nome) {
            alert('Nome da categoria é obrigatório');
            return;
        }

        // Desabilitar botão durante processamento
        const submitBtn = event.target.querySelector('button[type="submit"]');
        const originalText = submitBtn.textContent;
        submitBtn.disabled = true;
        submitBtn.textContent = 'Adicionando...';

        try {
            await window.api.createCategoria(nome);
            
            // Aguardar um pouco antes de recarregar para garantir commit no banco
            await new Promise(resolve => setTimeout(resolve, 500));
            
            // Recarregar apenas categorias, não todos os dados
            try {
                this.categorias = await window.api.getCategorias();
                this.renderCategorias();
            } catch (loadError) {
                console.error('Erro ao recarregar categorias:', loadError);
                // Tentar novamente após 1 segundo
                setTimeout(async () => {
                    try {
                        this.categorias = await window.api.getCategorias();
                        this.renderCategorias();
                    } catch (retryError) {
                        console.error('Erro na segunda tentativa:', retryError);
                    }
                }, 1000);
            }
            
            this.closeModal();
            this.showToast('Categoria criada com sucesso!', 'success');
        } catch (error) {
            console.error('Erro ao criar categoria:', error);
            
            // Tratamento específico para diferentes tipos de erro
            if (error.message.includes('409') || error.message.includes('já existe') || error.message.includes('duplicado')) {
                this.showToast('Já existe uma categoria com este nome', 'error');
            } else if (error.message.includes('401') || error.message.includes('não autorizado')) {
                this.showToast('Você não tem permissão para criar categorias', 'error');
            } else {
                this.showToast('Erro ao criar categoria: ' + (error.message || 'Erro desconhecido'), 'error');
            }
        } finally {
            // Restaurar botão
            submitBtn.disabled = false;
            submitBtn.textContent = originalText;
        }
    }

    async submitProduto(event) {
        event.preventDefault();
        const produto = {
            nome: document.getElementById('produto-nome').value,
            preco: parseFloat(document.getElementById('produto-preco').value),
            codigoBarras: document.getElementById('produto-codigo').value,
            categoriaId: document.getElementById('produto-categoria').value
        };

        try {
            await window.api.createProduto(produto);
            await this.loadData();
            this.renderProdutos();
            this.closeModal();
        } catch (error) {
            alert('Erro ao criar produto: ' + error.message);
        }
    }

    // Action methods
    async deleteCategoria(id) {
        if (confirm('Tem certeza que deseja excluir esta categoria?')) {
            try {
                await window.api.deleteCategoria(id);
                await this.loadData();
                this.renderCategorias();
            } catch (error) {
                alert('Erro ao excluir categoria: ' + error.message);
            }
        }
    }

    async deleteProduto(id) {
        if (confirm('Tem certeza que deseja excluir este produto?')) {
            try {
                await window.api.deleteProduto(id);
                await this.loadData();
                this.renderProdutos();
            } catch (error) {
                alert('Erro ao excluir produto: ' + error.message);
            }
        }
    }


    async refreshPedidos() {
        try {
            this.pedidos = await window.api.getPedidosAdmin();
            this.renderPedidos();
            await this.loadStats();
        } catch (error) {
            console.error('Erro ao atualizar pedidos:', error);
        }
    }

    startPedidosAutoRefresh() {
        this.stopPedidosAutoRefresh();
        this.pedidosRefreshInterval = setInterval(() => {
            this.refreshPedidos();
        }, 15000);
    }

    stopPedidosAutoRefresh() {
        if (this.pedidosRefreshInterval) {
            clearInterval(this.pedidosRefreshInterval);
            this.pedidosRefreshInterval = null;
        }
    }

    async editCategoria(id) {
        const categoria = this.categorias.find(c => c.id === id);
        if (!categoria) return;

        const nome = prompt('Novo nome da categoria:', categoria.nome);
        if (nome && nome !== categoria.nome) {
            try {
                await window.api.updateCategoria(id, nome);
                await this.loadData();
                this.renderCategorias();
            } catch (error) {
                alert('Erro ao atualizar categoria: ' + error.message);
            }
        }
    }

    async editProduto(id) {
        const produto = this.produtos.find(p => p.id === id);
        if (!produto) return;

        const nome = prompt('Novo nome do produto:', produto.nome);
        if (nome && nome !== produto.nome) {
            try {
                await window.api.updateProduto(id, { ...produto, nome });
                await this.loadData();
                this.renderProdutos();
            } catch (error) {
                alert('Erro ao atualizar produto: ' + error.message);
            }
        }
    }
}

// Global admin manager instance
window.adminManager = new AdminManager();