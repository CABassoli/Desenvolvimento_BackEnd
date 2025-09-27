// Customer dashboard management
class CustomerManager {
    constructor() {
        this.produtos = [];
        this.pedidos = [];
        this.carrinho = { itens: [] };
        this.expandedOrders = new Set();
        this.previousOrderStatuses = new Map();
    }

    // Date formatting function from admin.js
    fmtIso(s) {
        if (!s) return '-';
        const d = new Date(s);
        return isNaN(d) ? '-' : d.toLocaleDateString('pt-BR') + ' ' + d.toLocaleTimeString('pt-BR', {hour: '2-digit', minute: '2-digit'});
    }

    // Format date only
    fmtDate(s) {
        if (!s) return '-';
        const d = new Date(s);
        return isNaN(d) ? '-' : d.toLocaleDateString('pt-BR');
    }

    async loadDashboard() {
        await this.loadData();
        this.renderProdutos();
        this.renderPedidos();
        this.loadCarrinho();
        // Start auto-refresh for orders
        this.startOrderRefresh();
    }

    startOrderRefresh() {
        // Initialize previous statuses
        this.pedidos.forEach(pedido => {
            this.previousOrderStatuses.set(pedido.id, pedido.status);
        });
        
        // Refresh orders every 30 seconds to check for status updates
        setInterval(async () => {
            try {
                const updatedPedidos = await this.getCurrentUserPedidos();
                if (JSON.stringify(updatedPedidos) !== JSON.stringify(this.pedidos)) {
                    // Check for status changes and trigger notifications
                    updatedPedidos.forEach(updatedPedido => {
                        const previousStatus = this.previousOrderStatuses.get(updatedPedido.id);
                        if (previousStatus && previousStatus !== updatedPedido.status) {
                            this.handleStatusChange(updatedPedido, previousStatus, updatedPedido.status);
                        }
                        this.previousOrderStatuses.set(updatedPedido.id, updatedPedido.status);
                    });
                    
                    this.pedidos = updatedPedidos;
                    this.renderPedidos();
                }
            } catch (error) {
                console.error('Error refreshing orders:', error);
            }
        }, 30000);
    }
    
    handleStatusChange(pedido, oldStatus, newStatus) {
        const orderNumber = pedido.numero || pedido.id.substring(0, 8);
        
        switch(newStatus) {
            case 'PROCESSANDO':
                if (oldStatus === 'NOVO') {
                    this.showToast(`⏳ Pedido #${orderNumber} está sendo processado!`, 'info');
                }
                break;
            case 'PAGO':
                if (oldStatus === 'PROCESSANDO' || oldStatus === 'NOVO') {
                    this.showToast(`✅ Pagamento do pedido #${orderNumber} foi aprovado!`, 'success');
                }
                break;
            case 'ENVIADO':
                if (oldStatus === 'PAGO') {
                    this.showToast(`📦 Seu pedido #${orderNumber} foi enviado!`, 'success');
                }
                break;
            case 'ENTREGUE':
                if (oldStatus === 'ENVIADO') {
                    this.showToast(`🎉 Seu pedido #${orderNumber} foi entregue!`, 'success');
                }
                break;
            case 'CANCELADO':
                this.showToast(`❌ Pedido #${orderNumber} foi cancelado`, 'error');
                break;
        }
    }

    async loadData() {
        try {
            const [produtos, pedidos] = await Promise.all([
                window.api.getProdutos().catch(() => []),
                this.getCurrentUserPedidos().catch(() => [])
            ]);

            this.produtos = produtos || [];
            this.pedidos = pedidos || [];
        } catch (error) {
            console.error('Error loading customer data:', error);
        }
    }

    async getCurrentUserPedidos() {
        const pedidos = await window.api.getMeusPedidos(true);
        // Fetch payment info for each order if available
        if (pedidos && pedidos.length > 0) {
            for (let pedido of pedidos) {
                try {
                    // Try to get payment info for the order
                    pedido.pagamento = await this.getOrderPayment(pedido.id).catch(() => null);
                } catch (error) {
                    pedido.pagamento = null;
                }
            }
        }
        return pedidos;
    }

    async getOrderPayment(orderId) {
        try {
            const response = await window.api.request(`/api/pedidos/${orderId}/pagamento`);
            return response;
        } catch (error) {
            return null;
        }
    }

    toggleOrderExpanded(orderId) {
        if (this.expandedOrders.has(orderId)) {
            this.expandedOrders.delete(orderId);
        } else {
            this.expandedOrders.add(orderId);
        }
        this.renderPedidos();
    }

    getStatusInfo(status) {
        const statusMap = {
            'NOVO': { label: 'Novo', color: 'blue', icon: '🆕', progress: 10 },
            'PROCESSANDO': { label: 'Processando', color: 'yellow', icon: '⏳', progress: 30 },
            'PAGO': { label: 'Pago', color: 'green', icon: '✅', progress: 50 },
            'ENVIADO': { label: 'Enviado', color: 'indigo', icon: '📦', progress: 80 },
            'ENTREGUE': { label: 'Entregue', color: 'purple', icon: '🎉', progress: 100 },
            'CANCELADO': { label: 'Cancelado', color: 'red', icon: '❌', progress: 0 }
        };
        return statusMap[status] || { label: status, color: 'gray', icon: '❓', progress: 0 };
    }

    getPaymentMethodLabel(tipoPagamento) {
        const paymentMap = {
            'CARTAO': { label: 'Cartão de Crédito', icon: '💳' },
            'PIX': { label: 'PIX', icon: '📱' },
            'BOLETO': { label: 'Boleto Bancário', icon: '📄' }
        };
        return paymentMap[tipoPagamento] || { label: tipoPagamento || 'Não informado', icon: '💰' };
    }

    renderProdutos() {
        const container = document.getElementById('customer-produtos-list');
        
        if (this.produtos.length === 0) {
            container.innerHTML = '<p class="text-gray-500">Nenhum produto disponível.</p>';
            return;
        }

        container.innerHTML = `
            <div class="grid grid-cols-1 gap-4 max-h-96 overflow-y-auto">
                ${this.produtos.slice(0, 10).map(produto => {
                    const preco = new Intl.NumberFormat('pt-BR', { 
                        style: 'currency', 
                        currency: 'BRL' 
                    }).format(produto.preco);

                    return `
                        <div class="border rounded-lg p-4 hover:shadow-md transition-shadow">
                            <h4 class="font-semibold text-lg">${produto.nome}</h4>
                            <p class="text-blue-600 font-bold">${preco}</p>
                            <p class="text-sm text-gray-500">Código: ${produto.codigoBarras}</p>
                            <button onclick="window.customerManager.addToCart('${produto.id}')" 
                                    class="mt-2 bg-green-600 text-white px-3 py-1 rounded text-sm hover:bg-green-700">
                                Adicionar ao Carrinho
                            </button>
                        </div>
                    `;
                }).join('')}
            </div>
            ${this.produtos.length > 10 ? '<p class="text-sm text-gray-500 mt-2">Mostrando primeiros 10 produtos...</p>' : ''}
        `;
    }

    renderPedidos() {
        const container = document.getElementById('customer-pedidos-list');
        
        if (this.pedidos.length === 0) {
            container.innerHTML = '<p class="text-gray-500">Nenhum pedido encontrado.</p>';
            return;
        }

        const pedidosOrdenados = [...this.pedidos].sort((a, b) => 
            new Date(b.dataPedido) - new Date(a.dataPedido)
        );

        container.innerHTML = `
            <div class="orders-container">
                ${pedidosOrdenados.map(pedido => {
                    const valor = new Intl.NumberFormat('pt-BR', { 
                        style: 'currency', 
                        currency: 'BRL' 
                    }).format(pedido.valorTotal || pedido.total || 0);

                    const statusInfo = this.getStatusInfo(pedido.status);
                    const isExpanded = this.expandedOrders.has(pedido.id);
                    const orderNumber = pedido.numero || pedido.id.substring(0, 8);
                    const etaDate = pedido.etaEntrega ? this.fmtDate(pedido.etaEntrega) : null;
                    const orderDate = this.fmtIso(pedido.dataPedido);
                    const paymentInfo = pedido.pagamento ? this.getPaymentMethodLabel(pedido.pagamento.tipoPagamento) : { label: 'Aguardando', icon: '⏳' };

                    return `
                        <div class="order-card ${isExpanded ? 'expanded' : ''}">
                            <!-- Order Header -->
                            <div class="order-header" onclick="window.customerManager.toggleOrderExpanded('${pedido.id}')">
                                <div class="order-main-info">
                                    <div class="order-number-section">
                                        <h4 class="order-number">Pedido #${orderNumber}</h4>
                                        <span class="order-date">${orderDate}</span>
                                    </div>
                                    
                                    <div class="order-status-section">
                                        <span class="status-badge status-${statusInfo.color}">
                                            ${statusInfo.icon} ${statusInfo.label}
                                        </span>
                                        ${etaDate && pedido.status !== 'ENTREGUE' && pedido.status !== 'CANCELADO' ? `
                                            <div class="eta-info">
                                                <span class="eta-label">Entrega prevista:</span>
                                                <span class="eta-date">${etaDate}</span>
                                            </div>
                                        ` : ''}
                                    </div>

                                    <div class="order-value-section">
                                        <span class="order-total">${valor}</span>
                                        <span class="payment-method">${paymentInfo.icon} ${paymentInfo.label}</span>
                                    </div>

                                    <button class="expand-toggle">
                                        ${isExpanded ? '▲' : '▼'}
                                    </button>
                                </div>

                                <!-- Progress Bar -->
                                <div class="order-progress">
                                    <div class="progress-bar">
                                        <div class="progress-fill" style="width: ${statusInfo.progress}%"></div>
                                    </div>
                                    <div class="progress-steps">
                                        <div class="progress-step ${statusInfo.progress >= 10 ? 'completed' : ''}">
                                            <span class="step-dot"></span>
                                            <span class="step-label">Pedido</span>
                                        </div>
                                        <div class="progress-step ${statusInfo.progress >= 30 ? 'completed' : ''}">
                                            <span class="step-dot"></span>
                                            <span class="step-label">Processando</span>
                                        </div>
                                        <div class="progress-step ${statusInfo.progress >= 50 ? 'completed' : ''}">
                                            <span class="step-dot"></span>
                                            <span class="step-label">Pago</span>
                                        </div>
                                        <div class="progress-step ${statusInfo.progress >= 80 ? 'completed' : ''}">
                                            <span class="step-dot"></span>
                                            <span class="step-label">Enviado</span>
                                        </div>
                                        <div class="progress-step ${statusInfo.progress >= 100 ? 'completed' : ''}">
                                            <span class="step-dot"></span>
                                            <span class="step-label">Entregue</span>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <!-- Expandable Details -->
                            ${isExpanded ? `
                                <div class="order-details">
                                    <!-- Order Items -->
                                    ${pedido.itens && pedido.itens.length > 0 ? `
                                        <div class="detail-section">
                                            <h5 class="detail-title">Itens do Pedido</h5>
                                            <div class="items-list">
                                                ${pedido.itens.map(item => {
                                                    const itemSubtotal = new Intl.NumberFormat('pt-BR', { 
                                                        style: 'currency', 
                                                        currency: 'BRL' 
                                                    }).format(item.subtotal || (item.precoUnitario * item.quantidade));
                                                    return `
                                                        <div class="order-item">
                                                            <span class="item-name">${item.nome}</span>
                                                            <span class="item-quantity">Qtd: ${item.quantidade}</span>
                                                            <span class="item-price">${itemSubtotal}</span>
                                                        </div>
                                                    `;
                                                }).join('')}
                                            </div>
                                        </div>
                                    ` : ''}

                                    <!-- Delivery Address -->
                                    ${pedido.endereco ? `
                                        <div class="detail-section">
                                            <h5 class="detail-title">Endereço de Entrega</h5>
                                            <div class="address-info">
                                                <p>${pedido.endereco.logradouro}, ${pedido.endereco.numero}</p>
                                                ${pedido.endereco.complemento ? `<p>${pedido.endereco.complemento}</p>` : ''}
                                                <p>${pedido.endereco.bairro}</p>
                                                <p>${pedido.endereco.cidade} - ${pedido.endereco.estado}</p>
                                                <p>CEP: ${pedido.endereco.cep}</p>
                                            </div>
                                        </div>
                                    ` : ''}

                                    <!-- Payment Information -->
                                    ${pedido.pagamento ? `
                                        <div class="detail-section">
                                            <h5 class="detail-title">Informações de Pagamento</h5>
                                            <div class="payment-info">
                                                <p><strong>Método:</strong> ${paymentInfo.label}</p>
                                                ${pedido.paidAt ? `<p><strong>Pago em:</strong> ${this.fmtIso(pedido.paidAt)}</p>` : ''}
                                                ${pedido.pagamento.bandeira ? `<p><strong>Bandeira:</strong> ${pedido.pagamento.bandeira}</p>` : ''}
                                                <p><strong>Status:</strong> ${pedido.status === 'PAGO' || pedido.status === 'ENVIADO' || pedido.status === 'ENTREGUE' ? 'Confirmado' : 'Processando'}</p>
                                            </div>
                                        </div>
                                    ` : ''}

                                    <!-- Order Timeline -->
                                    <div class="detail-section">
                                        <h5 class="detail-title">Histórico do Pedido</h5>
                                        <div class="order-timeline">
                                            <div class="timeline-item">
                                                <span class="timeline-date">${this.fmtIso(pedido.createdAt || pedido.dataPedido)}</span>
                                                <span class="timeline-event">Pedido criado</span>
                                            </div>
                                            ${pedido.paidAt ? `
                                                <div class="timeline-item">
                                                    <span class="timeline-date">${this.fmtIso(pedido.paidAt)}</span>
                                                    <span class="timeline-event">Pagamento confirmado</span>
                                                </div>
                                            ` : ''}
                                            ${pedido.updatedAt && pedido.status === 'ENVIADO' ? `
                                                <div class="timeline-item">
                                                    <span class="timeline-date">${this.fmtIso(pedido.updatedAt)}</span>
                                                    <span class="timeline-event">Pedido enviado</span>
                                                </div>
                                            ` : ''}
                                            ${pedido.canceledAt ? `
                                                <div class="timeline-item">
                                                    <span class="timeline-date">${this.fmtIso(pedido.canceledAt)}</span>
                                                    <span class="timeline-event">Pedido cancelado</span>
                                                </div>
                                            ` : ''}
                                        </div>
                                    </div>
                                </div>
                            ` : ''}
                        </div>
                    `;
                }).join('')}
            </div>
        `;
    }

    async addToCart(produtoId) {
        try {
            // Use new method with JWT authentication
            await window.api.addItemCarrinho(produtoId, 1);
            
            // Show success message
            this.showToast('Produto adicionado ao carrinho com sucesso!', 'success');
            
            // Reload cart display
            this.loadCarrinho();
            
        } catch (error) {
            console.error('Error adding to cart:', error);
            this.showToast('Erro ao adicionar produto ao carrinho. Tente novamente.', 'error');
        }
    }

    async loadCarrinho() {
        try {
            // Use new method with JWT authentication
            const carrinho = await window.api.getCarrinho();
            this.carrinho = carrinho || { itens: [] };
            this.renderCarrinho(this.carrinho);
            this.updateCartDisplay();
            
        } catch (error) {
            console.error('Error loading cart:', error);
            const container = document.getElementById('customer-carrinho-list');
            if (container) {
                container.innerHTML = '<p class="text-red-500 text-center py-8">Erro ao carregar carrinho</p>';
            }
        }
    }

    renderCarrinho(carrinho) {
        const container = document.getElementById('customer-carrinho-list');
        const checkoutBtn = document.getElementById('checkout-btn');
        
        if (!carrinho || !carrinho.itens || carrinho.itens.length === 0) {
            container.innerHTML = '<p class="text-gray-500 text-center py-8">Carrinho vazio</p>';
            if (checkoutBtn) checkoutBtn.disabled = true;
            return;
        }
        
        // Adaptável para ambos os formatos de resposta

        container.innerHTML = `
            <div class="space-y-3 max-h-80 overflow-y-auto">
                ${carrinho.itens.map(item => {
                    // Compatibilidade com ambos os formatos de DTO
                    const nome = item.nome || (item.produto ? item.produto.nome : 'Produto');
                    const preco = item.preco || (item.produto ? item.produto.preco : 0);
                    const produtoId = item.produtoId || (item.produto ? item.produto.id : null);
                    const subtotal = new Intl.NumberFormat('pt-BR', { 
                        style: 'currency', 
                        currency: 'BRL' 
                    }).format((item.subtotal || (preco * item.quantidade)));

                    return `
                        <div class="flex items-center justify-between p-3 border border-gray-200 rounded">
                            <div class="flex-1">
                                <h4 class="font-semibold text-sm">${nome}</h4>
                                <p class="text-xs text-gray-500">${subtotal}</p>
                                <div class="flex items-center mt-2">
                                    <button onclick="window.customerManager.updateItemQuantity('${item.id}', ${item.quantidade - 1})" 
                                            class="bg-gray-200 text-gray-700 px-2 py-1 rounded text-xs hover:bg-gray-300"
                                            ${item.quantidade <= 1 ? 'disabled' : ''}>
                                        -
                                    </button>
                                    <span class="mx-2 text-sm font-bold">${item.quantidade}</span>
                                    <button onclick="window.customerManager.updateItemQuantity('${item.id}', ${item.quantidade + 1})" 
                                            class="bg-gray-200 text-gray-700 px-2 py-1 rounded text-xs hover:bg-gray-300">
                                        +
                                    </button>
                                </div>
                            </div>
                            <button onclick="window.customerManager.removeFromCart('${produtoId}')" 
                                    class="text-red-600 hover:text-red-800 p-1">
                                ❌
                            </button>
                        </div>
                    `;
                }).join('')}
            </div>
        `;
        
        if (checkoutBtn) checkoutBtn.disabled = false;
    }

    async updateItemQuantity(itemId, newQuantity) {
        if (newQuantity <= 0) {
            return this.removeFromCart(itemId);
        }

        try {
            const user = await window.api.getCurrentUser();
            if (!user) return;

            await window.api.updateCarrinhoItem(user.id, itemId, { quantidade: newQuantity });
            
            // Reload cart
            this.loadCarrinho();
            
        } catch (error) {
            console.error('Error updating item quantity:', error);
            this.showToast('Erro ao atualizar quantidade. Tente novamente.', 'error');
        }
    }

    async removeFromCart(produtoId) {
        if (!confirm('Deseja remover este item do carrinho?')) return;

        try {
            // Use new method with JWT authentication
            await window.api.removeItemCarrinho(produtoId);
            
            // Reload cart
            this.loadCarrinho();
            this.showToast('Item removido do carrinho com sucesso!', 'success');
            
        } catch (error) {
            console.error('Error removing item:', error);
            this.showToast('Erro ao remover item. Tente novamente.', 'error');
        }
    }

    async clearCart() {
        if (!confirm('Deseja limpar todo o carrinho? Esta ação não pode ser desfeita.')) return;

        try {
            // Use new method with JWT authentication
            await window.api.clearCarrinho();
            
            // Reload cart
            this.loadCarrinho();
            this.showToast('Carrinho limpo com sucesso!', 'success');
            
        } catch (error) {
            console.error('Error clearing cart:', error);
            this.showToast('Erro ao limpar carrinho. Tente novamente.', 'error');
        }
    }

    async viewCart() {
        // Scroll to cart section
        const cartSection = document.getElementById('customer-carrinho-list');
        if (cartSection) {
            cartSection.scrollIntoView({ behavior: 'smooth' });
        }
    }

    async checkout() {
        if (!this.carrinho.itens || this.carrinho.itens.length === 0) {
            this.showToast('Seu carrinho está vazio!', 'warning');
            return;
        }

        this.currentStep = 1;
        this.checkoutData = {
            endereco: null,
            pagamento: null,
            carrinho: this.carrinho
        };
        
        const modal = document.getElementById('checkout-modal');
        modal.classList.remove('hidden');
        
        this.renderCheckoutStep();
    }
    
    closeCheckout() {
        const modal = document.getElementById('checkout-modal');
        modal.classList.add('hidden');
        this.checkoutData = null;
        this.currentStep = 1;
    }
    
    updateStepIndicator(step) {
        const steps = document.querySelectorAll('.wizard-steps .step');
        steps.forEach((s, index) => {
            s.classList.remove('active', 'completed');
            if (index < step - 1) {
                s.classList.add('completed');
            } else if (index === step - 1) {
                s.classList.add('active');
            }
        });
        
        const prevBtn = document.getElementById('prev-step-btn');
        const nextBtn = document.getElementById('next-step-btn');
        
        if (step === 1) {
            prevBtn.classList.add('hidden');
        } else {
            prevBtn.classList.remove('hidden');
        }
        
        if (step === 3) {
            nextBtn.textContent = 'Confirmar Pedido';
            nextBtn.onclick = () => this.confirmarPedido();
        } else {
            nextBtn.textContent = 'Próximo';
            nextBtn.onclick = () => this.nextStep();
        }
    }
    
    async renderCheckoutStep() {
        this.updateStepIndicator(this.currentStep);
        const content = document.getElementById('checkout-content');
        
        switch(this.currentStep) {
            case 1:
                await this.renderAddressStep(content);
                break;
            case 2:
                await this.renderPaymentStep(content);
                break;
            case 3:
                await this.renderReviewStep(content);
                break;
        }
    }
    
    async renderAddressStep(content) {
        try {
            const enderecos = await window.api.getEnderecos();
            this.enderecos = enderecos || [];
            
            // Initialize stable CheckoutState
            window.CheckoutState = window.CheckoutState || {
                enderecoId: null,
                metodoPagamento: null,
                tokenCartao: null,
                bandeira: null,
                simulacaoStatus: null,
                total: 0
            };
            
            let addressContent = '';
            
            if (this.enderecos.length === 0) {
                addressContent = `
                    <div class="text-center py-8">
                        <p class="text-gray-600 mb-4">Nenhum endereço cadastrado</p>
                        <button onclick="window.customerManager.showAddAddressForm()" 
                                class="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700">
                            Cadastrar endereço
                        </button>
                    </div>
                `;
            } else {
                addressContent = `
                    <div class="space-y-3">
                        ${this.enderecos.map(end => `
                            <label class="address-radio-card block p-4 border rounded-lg cursor-pointer hover:bg-gray-50 
                                        ${window.CheckoutState.enderecoId === end.id ? 'border-blue-500 bg-blue-50' : 'border-gray-300'}">
                                <div class="flex items-start">
                                    <input type="radio" name="endereco" value="${end.id}" 
                                           ${window.CheckoutState.enderecoId === end.id ? 'checked' : ''}
                                           onchange="window.customerManager.selectAddress('${end.id}')"
                                           class="mt-1 mr-3">
                                    <div class="flex-1">
                                        ${end.ehPadrao ? '<span class="inline-block px-2 py-1 bg-green-100 text-green-800 text-xs rounded mb-1">Padrão</span>' : ''}
                                        <div class="font-semibold">${end.rua}, ${end.numero}</div>
                                        ${end.complemento ? `<div class="text-sm text-gray-600">${end.complemento}</div>` : ''}
                                        ${end.bairro ? `<div class="text-sm text-gray-600">${end.bairro}</div>` : ''}
                                        <div class="text-sm text-gray-600">${end.cidade}${end.estado ? ' - ' + end.estado : ''}</div>
                                        <div class="text-sm text-gray-600">CEP: ${this.formatCEP(end.cep)}</div>
                                    </div>
                                </div>
                            </label>
                        `).join('')}
                    </div>
                    <button onclick="window.customerManager.showAddAddressForm()" 
                            class="mt-4 text-blue-600 hover:underline">
                        + Cadastrar novo endereço
                    </button>
                `;
            }
            
            content.innerHTML = `
                <div class="space-y-4">
                    <h3 class="text-xl font-semibold mb-4">Selecione o endereço de entrega</h3>
                    ${addressContent}
                    <div id="address-form" class="hidden mt-4 p-4 bg-gray-50 rounded">
                    </div>
                </div>
            `;
            
            // Auto-select default address if none selected
            if (this.enderecos.length > 0 && !window.CheckoutState.enderecoId) {
                const defaultAddr = this.enderecos.find(e => e.ehPadrao) || this.enderecos[0];
                this.selectAddress(defaultAddr.id);
            }
            
            // Enable/disable next button based on selection
            this.updateNextButtonState();
            
        } catch (error) {
            console.error('Error loading addresses:', error);
            content.innerHTML = `
                <div class="space-y-4">
                    <h3 class="text-xl font-semibold mb-4">Selecione o endereço de entrega</h3>
                    <div class="text-center py-8">
                        <p class="text-red-500 mb-4">Erro ao carregar endereços</p>
                        <button onclick="window.customerManager.showAddAddressForm()" 
                                class="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700">
                            Cadastrar endereço
                        </button>
                    </div>
                    <div id="address-form" class="hidden mt-4 p-4 bg-gray-50 rounded">
                    </div>
                </div>
            `;
        }
    }
    
    selectAddress(addressId) {
        // Store in CheckoutState
        window.CheckoutState = window.CheckoutState || {
            enderecoId: null,
            metodoPagamento: null,
            tokenCartao: null,
            bandeira: null,
            simulacaoStatus: null,
            total: 0
        };
        window.CheckoutState.enderecoId = addressId;
        
        // Store the full address object in CheckoutState for faster access in review step
        const selectedAddress = this.enderecos.find(e => e.id === addressId);
        if (selectedAddress) {
            window.CheckoutState.endereco = selectedAddress;
        }
        
        // Also keep in checkoutData for compatibility
        this.checkoutData.endereco = selectedAddress;
        
        // Update UI
        document.querySelectorAll('.address-radio-card').forEach(card => {
            const radio = card.querySelector('input[type="radio"]');
            if (radio && radio.value === addressId) {
                card.classList.add('border-blue-500', 'bg-blue-50');
                card.classList.remove('border-gray-300');
            } else {
                card.classList.remove('border-blue-500', 'bg-blue-50');
                card.classList.add('border-gray-300');
            }
        });
        
        // Enable next button
        this.updateNextButtonState();
    }
    
    updateNextButtonState() {
        const nextBtn = document.getElementById('next-step-btn');
        if (nextBtn) {
            if (this.currentStep === 1) {
                nextBtn.disabled = !window.CheckoutState?.enderecoId;
                if (nextBtn.disabled) {
                    nextBtn.classList.add('opacity-50', 'cursor-not-allowed');
                } else {
                    nextBtn.classList.remove('opacity-50', 'cursor-not-allowed');
                }
            }
        }
    }
    
    showAddAddressForm() {
        const formDiv = document.getElementById('address-form');
        formDiv.classList.remove('hidden');
        formDiv.innerHTML = `
            <h4 class="font-semibold mb-3">Novo Endereço</h4>
            <form onsubmit="event.preventDefault(); window.customerManager.saveNewAddress()">
                <div class="grid grid-cols-2 gap-3">
                    <div class="col-span-2">
                        <label class="block text-sm font-medium mb-1">Rua*</label>
                        <input type="text" id="new-rua" required
                               class="w-full px-3 py-2 border rounded focus:outline-none focus:border-blue-500">
                    </div>
                    <div>
                        <label class="block text-sm font-medium mb-1">Número*</label>
                        <input type="text" id="new-numero" required
                               class="w-full px-3 py-2 border rounded focus:outline-none focus:border-blue-500">
                    </div>
                    <div>
                        <label class="block text-sm font-medium mb-1">Complemento</label>
                        <input type="text" id="new-complemento"
                               class="w-full px-3 py-2 border rounded focus:outline-none focus:border-blue-500">
                    </div>
                    <div>
                        <label class="block text-sm font-medium mb-1">Bairro</label>
                        <input type="text" id="new-bairro"
                               class="w-full px-3 py-2 border rounded focus:outline-none focus:border-blue-500">
                    </div>
                    <div>
                        <label class="block text-sm font-medium mb-1">Cidade*</label>
                        <input type="text" id="new-cidade" required
                               class="w-full px-3 py-2 border rounded focus:outline-none focus:border-blue-500">
                    </div>
                    <div>
                        <label class="block text-sm font-medium mb-1">Estado (UF)</label>
                        <input type="text" id="new-estado" maxlength="2" placeholder="SP"
                               style="text-transform: uppercase"
                               class="w-full px-3 py-2 border rounded focus:outline-none focus:border-blue-500">
                    </div>
                    <div>
                        <label class="block text-sm font-medium mb-1">CEP* (somente números)</label>
                        <input type="text" id="new-cep" maxlength="8" placeholder="12345678" required
                               onkeyup="this.value = this.value.replace(/\D/g, '')"
                               pattern="[0-9]{8}"
                               class="w-full px-3 py-2 border rounded focus:outline-none focus:border-blue-500">
                    </div>
                    <div class="col-span-2">
                        <label class="flex items-center">
                            <input type="checkbox" id="new-ehPadrao" class="mr-2">
                            <span class="text-sm">Definir como endereço padrão</span>
                        </label>
                    </div>
                </div>
                <div class="mt-4 flex space-x-2">
                    <button type="submit" class="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700">
                        Salvar Endereço
                    </button>
                    <button type="button" onclick="document.getElementById('address-form').classList.add('hidden')"
                            class="bg-gray-500 text-white px-4 py-2 rounded hover:bg-gray-600">
                        Cancelar
                    </button>
                </div>
            </form>
        `;
        
        // Focus on first field
        setTimeout(() => {
            document.getElementById('new-rua')?.focus();
        }, 100);
    }
    
    formatCepInput(input) {
        let value = input.value.replace(/\D/g, '');
        if (value.length > 5) {
            value = value.slice(0, 5) + '-' + value.slice(5, 8);
        }
        input.value = value;
    }
    
    formatCEP(cep) {
        if (!cep) return '';
        const cleaned = cep.toString().replace(/\D/g, '');
        if (cleaned.length === 8) {
            return cleaned.slice(0, 5) + '-' + cleaned.slice(5);
        }
        return cep;
    }
    
    async saveNewAddress() {
        const cep = document.getElementById('new-cep').value.replace(/\D/g, '');
        
        // Validate CEP has exactly 8 digits
        if (cep.length !== 8) {
            this.showToast('CEP deve ter exatamente 8 dígitos', 'error');
            return;
        }
        
        // Build DTO with correct field names for backend
        const dto = {
            rua: document.getElementById('new-rua').value.trim(),
            numero: document.getElementById('new-numero').value.trim(),
            complemento: document.getElementById('new-complemento').value.trim() || null,
            bairro: document.getElementById('new-bairro').value.trim() || null,
            cidade: document.getElementById('new-cidade').value.trim(),
            estado: document.getElementById('new-estado').value.trim().toUpperCase() || null,
            cep: cep, // Already cleaned, only digits
            ehPadrao: document.getElementById('new-ehPadrao').checked
        };
        
        try {
            const newAddress = await window.api.createEndereco(dto);
            this.showToast('Endereço adicionado com sucesso!', 'success');
            
            // Hide form
            document.getElementById('address-form').classList.add('hidden');
            
            // Reload addresses
            await this.renderAddressStep(document.getElementById('checkout-content'));
            
            // Select the new address
            this.selectAddress(newAddress.id);
        } catch (error) {
            console.error('Error saving address:', error);
            this.showToast(error.message || 'Erro ao salvar endereço', 'error');
        }
    }
    
    // Remove edit and delete functions as they are not needed in checkout
    // Only address selection and creation are required
    
    async renderPaymentStep(content) {
        // Initialize CheckoutState if needed
        if (!window.CheckoutState) {
            window.CheckoutState = {};
        }
        
        content.innerHTML = `
            <div class="space-y-4">
                <h3 class="text-xl font-semibold mb-4">Selecione a forma de pagamento</h3>
                
                <!-- Payment Methods -->
                <div class="space-y-3">
                    <!-- Credit Card Option -->
                    <label class="payment-method-card block p-4 border rounded-lg cursor-pointer hover:bg-gray-50
                                ${window.CheckoutState.metodoPagamento === 'CARTAO' ? 'border-blue-500 bg-blue-50' : 'border-gray-300'}">
                        <div class="flex items-start">
                            <input type="radio" name="payment" value="CARTAO" 
                                   ${window.CheckoutState.metodoPagamento === 'CARTAO' ? 'checked' : ''}
                                   onchange="window.customerManager.selectPaymentMethod('CARTAO')"
                                   class="mt-1 mr-3">
                            <div class="flex-1">
                                <div class="font-semibold flex items-center">
                                    💳 Cartão de Crédito
                                </div>
                                <p class="text-sm text-gray-600 mt-1">Pague em até 12x</p>
                            </div>
                        </div>
                    </label>
                    
                    <!-- Boleto Option -->
                    <label class="payment-method-card block p-4 border rounded-lg cursor-pointer hover:bg-gray-50
                                ${window.CheckoutState.metodoPagamento === 'BOLETO' ? 'border-blue-500 bg-blue-50' : 'border-gray-300'}">
                        <div class="flex items-start">
                            <input type="radio" name="payment" value="BOLETO" 
                                   ${window.CheckoutState.metodoPagamento === 'BOLETO' ? 'checked' : ''}
                                   onchange="window.customerManager.selectPaymentMethod('BOLETO')"
                                   class="mt-1 mr-3">
                            <div class="flex-1">
                                <div class="font-semibold flex items-center">
                                    📄 Boleto Bancário
                                </div>
                                <p class="text-sm text-gray-600 mt-1">Vencimento em 3 dias úteis</p>
                            </div>
                        </div>
                    </label>
                    
                    <!-- PIX Option -->
                    <label class="payment-method-card block p-4 border rounded-lg cursor-pointer hover:bg-gray-50
                                ${window.CheckoutState.metodoPagamento === 'PIX' ? 'border-blue-500 bg-blue-50' : 'border-gray-300'}">
                        <div class="flex items-start">
                            <input type="radio" name="payment" value="PIX" 
                                   ${window.CheckoutState.metodoPagamento === 'PIX' ? 'checked' : ''}
                                   onchange="window.customerManager.selectPaymentMethod('PIX')"
                                   class="mt-1 mr-3">
                            <div class="flex-1">
                                <div class="font-semibold flex items-center">
                                    📱 PIX
                                </div>
                                <p class="text-sm text-gray-600 mt-1">Pagamento instantâneo com 5% de desconto</p>
                            </div>
                        </div>
                    </label>
                </div>
                
                <!-- Payment Details Form -->
                <div id="payment-details" class="mt-6">
                    ${this.renderPaymentDetails(window.CheckoutState.metodoPagamento)}
                </div>
            </div>
        `;
        
        // Update next button state
        this.updatePaymentNextButton();
    }
    
    selectPaymentMethod(method) {
        // Initialize CheckoutState if needed
        window.CheckoutState = window.CheckoutState || {
            enderecoId: null,
            metodoPagamento: null,
            tokenCartao: null,
            bandeira: null,
            simulacaoStatus: null,
            total: 0
        };
        
        // Store the payment method with the correct enum value
        window.CheckoutState.metodoPagamento = method; // 'CARTAO', 'BOLETO', or 'PIX'
        
        // Reset payment tokens when method changes
        window.CheckoutState.tokenCartao = null;
        window.CheckoutState.bandeira = null;
        window.CheckoutState.simulacaoStatus = null;
        
        console.log('Payment method selected:', method);
        
        // Update payment details form
        const detailsDiv = document.getElementById('payment-details');
        if (detailsDiv) {
            detailsDiv.innerHTML = this.renderPaymentDetails(method);
        }
        
        // Update cards visual state
        document.querySelectorAll('.payment-method-card').forEach(card => {
            const radio = card.querySelector('input[type="radio"]');
            if (radio.value === method) {
                card.classList.remove('border-gray-300');
                card.classList.add('border-blue-500', 'bg-blue-50');
            } else {
                card.classList.remove('border-blue-500', 'bg-blue-50');
                card.classList.add('border-gray-300');
            }
        });
        
        // For PIX, make sure the generated code is available before updating button
        if (method === 'PIX' && window.CheckoutState.paymentData && window.CheckoutState.paymentData.pixCode) {
            // PIX code was already generated in renderPaymentDetails
            this.updatePaymentNextButton();
        } else {
            // Update next button
            this.updatePaymentNextButton();
        }
    }
    
    renderPaymentDetails(method) {
        if (!method) return '';
        
        // Auto-populate payment data when selecting method
        this.validatePaymentData();
        
        switch(method) {
            case 'CARTAO':
                return `
                    <div class="bg-gray-50 p-4 rounded-lg">
                        <h4 class="font-semibold mb-3">💳 Cartão de Crédito</h4>
                        <div class="text-center py-4">
                            <div class="bg-green-50 p-4 rounded-lg border border-green-200">
                                <p class="text-green-800 font-semibold">✅ Dados de pagamento simulados prontos!</p>
                                <p class="text-sm text-gray-600 mt-2">Clique em "Próximo" para revisar seu pedido</p>
                            </div>
                            <div class="mt-4 text-xs text-gray-500">
                                <p>Simulação: Cartão Visa terminado em 1111</p>
                                <p>Pagamento em até 12x sem juros</p>
                            </div>
                        </div>
                    </div>
                `;
                
            case 'BOLETO':
                return `
                    <div class="bg-gray-50 p-4 rounded-lg">
                        <h4 class="font-semibold mb-3">📄 Boleto Bancário</h4>
                        <div class="text-center py-4">
                            <div class="bg-green-50 p-4 rounded-lg border border-green-200">
                                <p class="text-green-800 font-semibold">✅ Boleto simulado pronto!</p>
                                <p class="text-sm text-gray-600 mt-2">Clique em "Próximo" para revisar seu pedido</p>
                            </div>
                            <div class="mt-4 text-xs text-gray-500">
                                <p>Vencimento: 3 dias úteis</p>
                                <p>Código de barras será gerado após confirmação</p>
                            </div>
                        </div>
                    </div>
                `;
                
            case 'PIX':
                const pixCode = this.generatePixCode();
                window.CheckoutState.paymentData = { pixCode };
                // For PIX, immediately enable the next button after generating the code
                setTimeout(() => this.updatePaymentNextButton(), 100);
                return `
                    <div class="bg-gray-50 p-4 rounded-lg">
                        <h4 class="font-semibold mb-3">📱 Pagamento via PIX</h4>
                        <div class="text-center">
                            <div class="bg-white p-4 rounded border-2 border-dashed border-gray-300">
                                <p class="text-sm text-gray-600 mb-2">Código PIX (simulado):</p>
                                <code class="block bg-gray-100 p-3 rounded text-xs break-all">${pixCode}</code>
                            </div>
                            <div class="bg-green-50 p-4 rounded-lg border border-green-200 mt-4">
                                <p class="text-green-800 font-semibold">✅ Código gerado! Pronto para prosseguir.</p>
                                <p class="text-sm text-gray-600 mt-1">Será processado na confirmação do pedido</p>
                            </div>
                        </div>
                    </div>
                `;
                
            default:
                return '';
        }
    }
    
    formatCardNumber(input) {
        let value = input.value.replace(/\s/g, '');
        let formatted = value.match(/.{1,4}/g)?.join(' ') || value;
        input.value = formatted;
    }
    
    formatCardExpiry(input) {
        let value = input.value.replace(/\D/g, '');
        if (value.length >= 2) {
            value = value.substring(0, 2) + '/' + value.substring(2, 4);
        }
        input.value = value;
    }
    
    formatCPF(input) {
        let value = input.value.replace(/\D/g, '');
        if (value.length > 11) value = value.substring(0, 11);
        
        if (value.length > 9) {
            value = value.substring(0, 3) + '.' + value.substring(3, 6) + '.' + value.substring(6, 9) + '-' + value.substring(9);
        } else if (value.length > 6) {
            value = value.substring(0, 3) + '.' + value.substring(3, 6) + '.' + value.substring(6);
        } else if (value.length > 3) {
            value = value.substring(0, 3) + '.' + value.substring(3);
        }
        input.value = value;
    }
    
    generatePixCode() {
        // Generate a simulated PIX code
        const chars = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ';
        let code = 'PIX';
        for (let i = 0; i < 32; i++) {
            code += chars.charAt(Math.floor(Math.random() * chars.length));
        }
        return code;
    }
    
    generateIdempotencyKey() {
        // Generate a unique key to prevent duplicate orders
        return 'IDK-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
    }
    
    validatePaymentData() {
        const method = window.CheckoutState?.paymentMethod;
        if (!method) return false;
        
        // Ensure CheckoutState and paymentData exist
        if (!window.CheckoutState) {
            window.CheckoutState = {};
        }
        window.CheckoutState.paymentData = window.CheckoutState.paymentData || {};
        
        // Auto-generate simulated data based on payment method
        switch(method) {
            case 'CARTAO':
                // Auto-generate simulated card data if not present
                if (!window.CheckoutState.paymentData.cardNumber) {
                    window.CheckoutState.paymentData.cardNumber = '4111111111111111';
                    window.CheckoutState.paymentData.cardName = 'SIMULADO';
                    window.CheckoutState.paymentData.cardExpiry = '12/25';
                    window.CheckoutState.paymentData.cardCvv = '123';
                }
                break;
                
            case 'BOLETO':
                // Auto-generate simulated boleto data if not present
                if (!window.CheckoutState.paymentData.cpf) {
                    window.CheckoutState.paymentData.cpf = '12345678900';
                    window.CheckoutState.paymentData.boletoCode = 'BOLETO-' + Date.now();
                }
                break;
                
            case 'PIX':
                // PIX code is already generated in renderPaymentDetails
                if (!window.CheckoutState.paymentData.pixCode) {
                    window.CheckoutState.paymentData.pixCode = this.generatePixCode();
                }
                break;
        }
        
        // Always return true - validation passed
        return true;
    }
    
    updatePaymentNextButton() {
        const nextBtn = document.getElementById('next-step-btn');
        if (nextBtn && this.currentStep === 2) {
            // Simply check if payment method is selected
            const hasPaymentMethod = !!window.CheckoutState.metodoPagamento;
            nextBtn.disabled = !hasPaymentMethod;
            if (nextBtn.disabled) {
                nextBtn.classList.add('opacity-50', 'cursor-not-allowed');
            } else {
                nextBtn.classList.remove('opacity-50', 'cursor-not-allowed');
            }
        }
    }
    
    async selectPayment(tipo) {
        this.checkoutData.pagamento = { tipo: tipo };
        
        document.querySelectorAll('.payment-card').forEach(card => {
            card.classList.remove('selected');
        });
        event.currentTarget?.classList.add('selected');
        
        const simulationDiv = document.getElementById('payment-simulation');
        simulationDiv.innerHTML = '<p class="text-gray-600">Simulando pagamento...</p>';
        
        try {
            const total = this.carrinho.itens.reduce((sum, item) => sum + (item.subtotal || item.preco * item.quantidade), 0);
            
            const payload = {
                tipoPagamento: tipo,
                valor: total
            };
            
            let attempts = 0;
            let simulationResult = null;
            
            while (attempts < 3 && !simulationResult) {
                try {
                    const result = await window.api.simularPagamento(payload);
                    if (result.aprovado) {
                        simulationResult = result;
                        break;
                    }
                    attempts++;
                    if (attempts < 3) {
                        simulationDiv.innerHTML = `<p class="text-yellow-600">Tentativa ${attempts} falhou. Tentando novamente...</p>`;
                        await new Promise(resolve => setTimeout(resolve, 1000));
                    }
                } catch (error) {
                    attempts++;
                    if (attempts >= 3) {
                        throw error;
                    }
                }
            }
            
            if (simulationResult) {
                this.checkoutData.pagamento.simulacao = simulationResult;
                simulationDiv.innerHTML = `
                    <div class="bg-green-50 border border-green-200 rounded p-4">
                        <div class="text-green-800 font-semibold">✅ Pagamento Aprovado!</div>
                        <div class="text-sm text-green-600 mt-1">ID da transação: ${simulationResult.transacaoId}</div>
                        <div class="text-sm text-green-600">Taxa: ${new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(simulationResult.taxaProcessamento)}</div>
                    </div>
                `;
            } else {
                simulationDiv.innerHTML = `
                    <div class="bg-red-50 border border-red-200 rounded p-4">
                        <div class="text-red-800 font-semibold">❌ Pagamento Recusado</div>
                        <div class="text-sm text-red-600 mt-1">Por favor, tente outro método de pagamento</div>
                    </div>
                `;
                this.checkoutData.pagamento = null;
            }
        } catch (error) {
            console.error('Payment simulation error:', error);
            simulationDiv.innerHTML = `
                <div class="bg-red-50 border border-red-200 rounded p-4">
                    <div class="text-red-800">Erro ao simular pagamento</div>
                </div>
            `;
            this.checkoutData.pagamento = null;
        }
    }
    
    async renderReviewStep(content) {
        // Load cart if not loaded
        if (!this.carrinho?.itens || this.carrinho.itens.length === 0) {
            await this.loadCarrinho();
        }
        
        // Get selected address - use cached data first (faster!)
        let selectedAddress = null;
        if (window.CheckoutState?.endereco) {
            // Use cached address from CheckoutState (no API call needed)
            selectedAddress = window.CheckoutState.endereco;
        } else if (window.CheckoutState?.enderecoId) {
            // Only call API if we have ID but no cached address
            try {
                selectedAddress = await window.api.getEndereco(window.CheckoutState.enderecoId);
                // Cache it for future use
                window.CheckoutState.endereco = selectedAddress;
            } catch (error) {
                console.error('Error loading address:', error);
            }
        }
        
        // Calculate totals
        const subtotal = this.carrinho.itens.reduce((sum, item) => {
            return sum + (item.subtotal || item.preco * item.quantidade);
        }, 0);
        const frete = 15.00; // Fixed shipping cost
        const total = subtotal + frete;
        
        // Apply PIX discount if applicable
        let discount = 0;
        let finalTotal = total;
        if (window.CheckoutState?.paymentMethod === 'PIX') {
            discount = total * 0.05; // 5% discount for PIX
            finalTotal = total - discount;
        }
        
        // Format payment method label
        const paymentMethodLabels = {
            'CARTAO': '💳 Cartão de Crédito',
            'BOLETO': '📄 Boleto Bancário',
            'PIX': '📱 PIX'
        };
        
        content.innerHTML = `
            <div class="space-y-4">
                <h3 class="text-xl font-semibold mb-4">Revise e Confirme seu Pedido</h3>
                
                <!-- Shipping Address -->
                <div class="bg-white border rounded-lg p-4">
                    <h4 class="font-semibold mb-3 flex items-center">
                        📍 Endereço de Entrega
                    </h4>
                    ${selectedAddress ? `
                        <div class="text-gray-700">
                            <p class="font-medium">${selectedAddress.rua}, ${selectedAddress.numero}</p>
                            ${selectedAddress.complemento ? `<p class="text-sm">${selectedAddress.complemento}</p>` : ''}
                            <p class="text-sm">${selectedAddress.bairro}</p>
                            <p class="text-sm">${selectedAddress.cidade} - ${selectedAddress.estado}</p>
                            <p class="text-sm">CEP: ${selectedAddress.cep}</p>
                        </div>
                    ` : `
                        <p class="text-red-600">⚠️ Endereço não selecionado</p>
                    `}
                </div>
                
                <!-- Payment Method -->
                <div class="bg-white border rounded-lg p-4">
                    <h4 class="font-semibold mb-3 flex items-center">
                        💳 Forma de Pagamento
                    </h4>
                    <p class="text-gray-700">
                        ${paymentMethodLabels[window.CheckoutState?.paymentMethod] || 'Não selecionado'}
                    </p>
                    ${this.renderPaymentSummary()}
                </div>
                
                <!-- Order Items -->
                <div class="bg-white border rounded-lg p-4">
                    <h4 class="font-semibold mb-3 flex items-center">
                        🛒 Itens do Pedido
                    </h4>
                    <div class="space-y-2 max-h-60 overflow-y-auto">
                        ${this.carrinho.itens.map(item => `
                            <div class="flex justify-between items-center py-2 border-b last:border-0">
                                <div class="flex-1">
                                    <p class="font-medium">${item.nome || item.produto?.nome || 'Produto'}</p>
                                    <p class="text-sm text-gray-600">Quantidade: ${item.quantidade}</p>
                                </div>
                                <div class="text-right">
                                    <p class="font-semibold">
                                        ${new Intl.NumberFormat('pt-BR', { 
                                            style: 'currency', 
                                            currency: 'BRL' 
                                        }).format(item.subtotal || item.preco * item.quantidade)}
                                    </p>
                                </div>
                            </div>
                        `).join('')}
                    </div>
                </div>
                
                <!-- Order Summary -->
                <div class="bg-gray-50 border rounded-lg p-4">
                    <h4 class="font-semibold mb-3">Resumo do Pedido</h4>
                    <div class="space-y-2">
                        <div class="flex justify-between">
                            <span>Subtotal (${this.carrinho.itens.length} ${this.carrinho.itens.length === 1 ? 'item' : 'itens'}):</span>
                            <span>${new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(subtotal)}</span>
                        </div>
                        <div class="flex justify-between">
                            <span>Frete:</span>
                            <span>${new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(frete)}</span>
                        </div>
                        ${discount > 0 ? `
                            <div class="flex justify-between text-green-600">
                                <span>Desconto PIX (5%):</span>
                                <span>-${new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(discount)}</span>
                            </div>
                        ` : ''}
                        <div class="border-t pt-2 flex justify-between text-lg font-bold">
                            <span>Total:</span>
                            <span class="text-blue-600">
                                ${new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(finalTotal)}
                            </span>
                        </div>
                    </div>
                </div>
                
                <!-- Delivery Estimate -->
                <div class="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                    <p class="text-sm flex items-center">
                        📅 Previsão de entrega: <strong class="ml-2">7 a 10 dias úteis</strong>
                    </p>
                </div>
            </div>
        `;
    }
    
    renderPaymentSummary() {
        const method = window.CheckoutState?.paymentMethod;
        const data = window.CheckoutState?.paymentData || {};
        
        if (!method) return '';
        
        switch(method) {
            case 'CARTAO':
                if (data.cardNumber) {
                    const lastFour = data.cardNumber.slice(-4);
                    return `
                        <div class="mt-2 text-sm text-gray-600">
                            <p>Cartão terminado em: **** ${lastFour}</p>
                            <p>Titular: ${data.cardName || ''}</p>
                        </div>
                    `;
                }
                break;
            case 'BOLETO':
                if (data.cpf) {
                    const maskedCpf = data.cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.***.***-$4');
                    return `
                        <div class="mt-2 text-sm text-gray-600">
                            <p>CPF: ${maskedCpf}</p>
                            <p>Vencimento: 3 dias úteis</p>
                        </div>
                    `;
                }
                break;
            case 'PIX':
                if (data.pixCode) {
                    return `
                        <div class="mt-2 text-sm text-gray-600">
                            <p>Código PIX gerado</p>
                            <p>Pagamento instantâneo com desconto</p>
                        </div>
                    `;
                }
                break;
        }
        return '';
    }
    
    async handleCheckoutConfirm() {
        const confirmBtn = document.getElementById('confirm-order-btn');
        if (confirmBtn) {
            confirmBtn.disabled = true;
            confirmBtn.innerHTML = '⏳ Processando pedido...';
        }
        
        try {
            // Validate required data
            if (!window.CheckoutState?.enderecoId) {
                throw new Error('Por favor, selecione um endereço de entrega');
            }
            
            if (!window.CheckoutState?.metodoPagamento) {
                throw new Error('Por favor, selecione uma forma de pagamento');
            }
            
            // Load cart to get the total
            if (!this.carrinho?.itens || this.carrinho.itens.length === 0) {
                await this.loadCarrinho();
            }
            
            if (this.carrinho.itens.length === 0) {
                throw new Error('Carrinho está vazio');
            }
            
            // Calculate total with shipping
            const subtotal = this.carrinho.itens.reduce((sum, item) => {
                return sum + (item.subtotal || item.preco * item.quantidade);
            }, 0);
            const frete = 15.00;
            let finalTotal = subtotal + frete;
            
            // Apply PIX discount
            if (window.CheckoutState.metodoPagamento === 'PIX') {
                finalTotal = finalTotal * 0.95; // 5% discount
            }
            
            // First, simulate payment
            console.log('Simulating payment...');
            const paymentSimulation = await this.simulatePayment(finalTotal);
            
            if (paymentSimulation.status !== 'APROVADO') {
                throw new Error('Pagamento não foi aprovado. Tente novamente.');
            }
            
            // Create order confirmation request
            const confirmRequest = {
                enderecoId: window.CheckoutState.enderecoId,
                simulacaoPagamento: paymentSimulation,
                idempotencyKey: 'order-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9)
            };
            
            console.log('Confirming order with data:', confirmRequest);
            
            // Call order confirmation endpoint
            const response = await window.api.request('/api/pedidos/confirmar', {
                method: 'POST',
                body: JSON.stringify(confirmRequest)
            });
            
            console.log('Order confirmed:', response);
            
            // Clear cart and checkout state
            window.cart = [];
            this.carrinho = { itens: [] };
            window.CheckoutState = {};
            localStorage.removeItem('cart');
            
            // Show success message
            this.showToast(`✅ Pedido #${response.numero || response.id.substring(0, 8)} confirmado com sucesso!`, 'success');
            
            // Close modal and refresh orders
            this.closeCheckoutModal();
            await this.loadDashboard();
            
            // Scroll to orders section
            const ordersSection = document.getElementById('customer-pedidos');
            if (ordersSection) {
                ordersSection.scrollIntoView({ behavior: 'smooth' });
            }
            
        } catch (error) {
            console.error('Error confirming order:', error);
            this.showToast('❌ ' + (error.message || 'Erro ao confirmar pedido'), 'error');
            
            // Re-enable button
            if (confirmBtn) {
                confirmBtn.disabled = false;
                confirmBtn.innerHTML = '✅ Confirmar Simulação';
            }
        }
    }
    
    async simulatePayment(amount) {
        const paymentData = window.CheckoutState.paymentData || {};
        
        // Prepare payment simulation request
        const simulationRequest = {
            metodo: window.CheckoutState.metodoPagamento,
            valor: amount
        };
        
        // Add card data if payment method is credit card
        if (window.CheckoutState.metodoPagamento === 'CARTAO' && paymentData.cardNumber) {
            simulationRequest.dadosCartao = {
                numero: paymentData.cardNumber,
                nome: paymentData.cardName,
                validade: paymentData.cardExpiry,
                cvv: paymentData.cardCvv,
                bandeira: this.detectCardBrand(paymentData.cardNumber)
            };
        }
        
        // Simulate payment
        const response = await window.api.request('/api/pagamentos/simular', {
            method: 'POST',
            body: JSON.stringify(simulationRequest)
        });
        
        return response;
    }
    
    detectCardBrand(cardNumber) {
        // Simple card brand detection
        const firstDigit = cardNumber.charAt(0);
        const firstTwo = cardNumber.substring(0, 2);
        
        if (firstDigit === '4') return 'VISA';
        if (['51', '52', '53', '54', '55'].includes(firstTwo)) return 'MASTERCARD';
        if (['34', '37'].includes(firstTwo)) return 'AMEX';
        if (firstTwo === '36') return 'DINERS';
        if (firstTwo === '65' || firstTwo === '60') return 'DISCOVER';
        
        return 'OTHER';
    }
    
    async nextStep() {
        if (this.currentStep === 1) {
            // Check if address is selected using the new state system
            if (!window.CheckoutState.enderecoId) {
                this.showToast('Por favor, selecione um endereço', 'warning');
                return;
            }
        } else if (this.currentStep === 2) {
            // Just check if payment method is selected
            if (!window.CheckoutState.metodoPagamento) {
                this.showToast('Por favor, selecione uma forma de pagamento', 'warning');
                return;
            }
            
            // Auto-validate and populate payment data
            this.validatePaymentData();
        }
        
        this.currentStep++;
        this.renderCheckoutStep();
    }
    
    prevStep() {
        if (this.currentStep > 1) {
            this.currentStep--;
            this.renderCheckoutStep();
        }
    }
    
    async confirmarPedido() {
        console.log('🚀 INICIO - Processo de confirmação de pedido');
        const nextBtn = document.getElementById('next-step-btn');
        nextBtn.disabled = true;
        nextBtn.textContent = 'Processando pedido...';
        
        try {
            // Validate cart is not empty
            if (!this.carrinho || !this.carrinho.itens || this.carrinho.itens.length === 0) {
                throw new Error('Carrinho vazio ou não encontrado');
            }
            
            // Validate required data
            if (!window.CheckoutState?.enderecoId) {
                throw new Error('Por favor, selecione um endereço de entrega');
            }
            
            if (!window.CheckoutState?.metodoPagamento) {
                throw new Error('Por favor, selecione uma forma de pagamento');
            }
            
            // Calculate cart total
            const cartTotal = this.carrinho.itens.reduce((sum, item) => {
                return sum + (item.subtotal || item.preco * item.quantidade);
            }, 0) + 15.00; // Add shipping cost
            
            // Apply PIX discount if applicable
            let finalTotal = cartTotal;
            if (window.CheckoutState.metodoPagamento === 'PIX') {
                finalTotal = cartTotal * 0.95; // 5% discount
            }
            
            // Simulate the payment through the backend
            console.log('Simulating payment with method:', window.CheckoutState.metodoPagamento);
            const simulationRequest = {
                metodo: window.CheckoutState.metodoPagamento, // This will be 'CARTAO', 'BOLETO' or 'PIX'
                valor: finalTotal
            };
            
            // Add card data if payment method is credit card
            if (window.CheckoutState.metodoPagamento === 'CARTAO') {
                const paymentData = window.CheckoutState.paymentData || {};
                simulationRequest.dadosCartao = {
                    numero: paymentData.cardNumber || '4111111111111111',
                    nome: paymentData.cardName || 'SIMULADO',
                    validade: paymentData.cardExpiry || '12/25',
                    cvv: paymentData.cardCvv || '123',
                    bandeira: 'VISA'
                };
            }
            
            // Call payment simulation endpoint
            const simulacaoPagamento = await window.api.simularPagamento(simulationRequest);
            console.log('Payment simulation response:', simulacaoPagamento);
            
            // Check if payment was approved (for credit card) or pending (for boleto/pix)
            if (simulacaoPagamento.status !== 'APROVADO' && simulacaoPagamento.status !== 'PENDENTE') {
                throw new Error('Pagamento não foi aprovado. Tente outro método de pagamento.');
            }
            
            // Now create a real order with the simulation data
            console.log('Payment simulation successful, creating real order...');
            
            // Prepare order confirmation payload with NEW contract structure
            const confirmarPedidoRequest = {
                enderecoId: window.CheckoutState.enderecoId,
                pagamento: {
                    metodo: window.CheckoutState.metodoPagamento // 'CARTAO', 'BOLETO', 'PIX'
                },
                idempotencyKey: 'checkout-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9)
            };
            
            // Add card-specific data if method is CARTAO
            if (window.CheckoutState.metodoPagamento === 'CARTAO' && simulacaoPagamento.tokenCartao) {
                confirmarPedidoRequest.pagamento.tokenCartao = simulacaoPagamento.tokenCartao;
                confirmarPedidoRequest.pagamento.bandeira = simulacaoPagamento.bandeira || 'VISA';
            }
            
            console.log('📦 CRIANDO PEDIDO - Payload:', confirmarPedidoRequest);
            
            // Call the backend to create the real order
            const pedidoCriado = await window.api.confirmarPedido(confirmarPedidoRequest);
            console.log('✅ PEDIDO CRIADO COM SUCESSO:', pedidoCriado);
            
            // Show multiple success feedbacks
            this.showToast('✅ Pedido criado com sucesso!', 'success');
            
            // Show success modal with order details
            this.showOrderSuccessModal(pedidoCriado);
            
            // IMPORTANTE: Limpar carrinho no backend primeiro
            console.log('🧹 LIMPANDO CARRINHO NO BACKEND...');
            try {
                const clearResponse = await fetch('/api/carrinho', {
                    method: 'DELETE',
                    headers: authHeaders()
                });
                
                if (clearResponse.ok) {
                    console.log('✅ Carrinho limpo no backend com sucesso');
                } else {
                    console.warn('⚠️ Aviso: Erro ao limpar carrinho no backend:', clearResponse.status);
                }
            } catch (clearError) {
                console.error('❌ Erro ao limpar carrinho no backend:', clearError);
                // Continua mesmo se falhar - o pedido já foi criado
            }
            
            // Limpar carrinho localmente
            console.log('🧹 Limpando carrinho localmente...');
            window.cart = [];
            this.carrinho = { itens: [] };
            localStorage.removeItem('cart');
            
            // Reset checkout state
            window.CheckoutState = {};
            this.checkoutData = null;
            
            // Atualizar contador do carrinho
            this.updateCartCount();
            
            // Aguardar um pouco para o usuário ver a mensagem de sucesso
            console.log('⏳ Aguardando 3 segundos antes de fechar modal e recarregar...');
            setTimeout(() => {
                // Close the checkout modal
                this.closeCheckoutModal();
                
                // Reload to show the new order in the list
                console.log('🔄 Recarregando página para mostrar o novo pedido...');
                window.location.reload();
            }, 3000);
            
        } catch (error) {
            console.error('❌ ERRO AO CRIAR PEDIDO:', error);
            console.error('Detalhes do erro:', {
                message: error.message,
                stack: error.stack,
                response: error.response
            });
            
            // Mensagem de erro mais específica
            let errorMessage = 'Erro ao criar pedido. ';
            if (error.message) {
                if (error.message.includes('Carrinho vazio')) {
                    errorMessage = 'Seu carrinho está vazio. Adicione produtos antes de finalizar a compra.';
                } else if (error.message.includes('endereço')) {
                    errorMessage = 'Por favor, selecione um endereço de entrega válido.';
                } else if (error.message.includes('pagamento')) {
                    errorMessage = 'Erro ao processar pagamento. Verifique os dados do cartão.';
                } else {
                    errorMessage += error.message;
                }
            } else {
                errorMessage += 'Por favor, tente novamente.';
            }
            
            this.showToast('❌ ' + errorMessage, 'error');
            
            // Mostrar alerta adicional para garantir que o usuário veja o erro
            alert('Erro ao processar pedido:\n\n' + errorMessage);
            
            nextBtn.disabled = false;
            nextBtn.textContent = 'Confirmar Pedido';
        }
    }
    
    showOrderSuccessModal(pedido) {
        // Criar um modal de sucesso mais visível
        const modal = document.createElement('div');
        modal.className = 'fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50';
        modal.style.cssText = 'position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 9999;';
        
        const orderNumber = pedido.numero || pedido.id.substring(0, 8).toUpperCase();
        const valor = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(pedido.valorTotal || pedido.total);
        
        modal.innerHTML = `
            <div style="background: white; border-radius: 12px; padding: 32px; max-width: 500px; text-align: center; box-shadow: 0 20px 60px rgba(0,0,0,0.3);">
                <div style="font-size: 64px; margin-bottom: 20px;">🎉</div>
                <h2 style="color: #10b981; font-size: 28px; font-weight: bold; margin-bottom: 16px;">Pedido Confirmado!</h2>
                <p style="font-size: 18px; color: #374151; margin-bottom: 24px;">Seu pedido foi criado com sucesso!</p>
                
                <div style="background: #f3f4f6; border-radius: 8px; padding: 16px; margin-bottom: 24px; text-align: left;">
                    <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
                        <span style="font-weight: bold;">Número do Pedido:</span>
                        <span style="color: #1f2937;">#${orderNumber}</span>
                    </div>
                    <div style="display: flex; justify-content: space-between;">
                        <span style="font-weight: bold;">Valor Total:</span>
                        <span style="color: #10b981; font-weight: bold;">${valor}</span>
                    </div>
                </div>
                
                <p style="font-size: 14px; color: #6b7280; margin-bottom: 16px;">Você pode acompanhar seu pedido na seção "Meus Pedidos"</p>
                
                <div style="font-size: 14px; color: #9ca3af; margin-top: 20px;">
                    Fechando em <span id="success-countdown">3</span> segundos...
                </div>
            </div>
        `;
        
        document.body.appendChild(modal);
        
        // Animação de entrada
        setTimeout(() => {
            modal.querySelector('div').style.transform = 'scale(1.05)';
            setTimeout(() => {
                modal.querySelector('div').style.transform = 'scale(1)';
            }, 200);
        }, 10);
        
        // Countdown visual
        let countdown = 3;
        const countdownElement = document.getElementById('success-countdown');
        const countdownInterval = setInterval(() => {
            countdown--;
            if (countdownElement) {
                countdownElement.textContent = countdown;
            }
            if (countdown <= 0) {
                clearInterval(countdownInterval);
                if (modal.parentNode) {
                    modal.remove();
                }
            }
        }, 1000);
        
        // Remover modal após 3 segundos
        setTimeout(() => {
            if (modal.parentNode) {
                modal.remove();
            }
        }, 3000);
    }
    
    updateCartCount() {
        // Atualizar contador do carrinho no header/navbar se existir
        const cartCountElements = document.querySelectorAll('.cart-count, #cart-count, [data-cart-count]');
        const itemCount = (window.cart || []).reduce((sum, item) => sum + (item.quantidade || 0), 0);
        
        cartCountElements.forEach(el => {
            el.textContent = itemCount;
            if (itemCount === 0) {
                el.style.display = 'none';
            } else {
                el.style.display = '';
            }
        });
        
        console.log(`🛒 Contador do carrinho atualizado: ${itemCount} itens`);
    }
    
    showSuccessPage(pedido) {
        const content = document.getElementById('checkout-content');
        const eta = new Date(pedido.etaEntrega);
        
        content.innerHTML = `
            <div class="text-center py-8">
                <div class="success-icon">✅</div>
                <div class="success-message">Pedido Confirmado com Sucesso!</div>
                
                <div class="order-details max-w-md mx-auto">
                    <div class="text-left space-y-2">
                        <div class="flex justify-between">
                            <span class="font-semibold">Número do Pedido:</span>
                            <span>#${pedido.numero || pedido.id.substring(0, 8).toUpperCase()}</span>
                        </div>
                        <div class="flex justify-between">
                            <span class="font-semibold">Data da Compra:</span>
                            <span>${new Date(pedido.dataPedido).toLocaleDateString('pt-BR')}</span>
                        </div>
                        <div class="flex justify-between">
                            <span class="font-semibold">Valor Total:</span>
                            <span>${new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(pedido.valorTotal)}</span>
                        </div>
                        <div class="flex justify-between">
                            <span class="font-semibold">Previsão de Entrega:</span>
                            <span>${eta.toLocaleDateString('pt-BR')}</span>
                        </div>
                        <div class="flex justify-between">
                            <span class="font-semibold">Status:</span>
                            <span class="status-processando">${pedido.status}</span>
                        </div>
                    </div>
                </div>
                
                <div class="mt-8">
                    <button onclick="window.customerManager.closeCheckout()" 
                            class="bg-blue-600 text-white px-6 py-3 rounded hover:bg-blue-700">
                        Voltar às Compras
                    </button>
                </div>
            </div>
        `;
        
        document.getElementById('prev-step-btn').classList.add('hidden');
        document.getElementById('next-step-btn').classList.add('hidden');
        document.querySelectorAll('.wizard-steps .step').forEach(s => s.classList.add('completed'));
    }
    
    refreshPedidos() {
        this.renderPedidos();
    }
    
    renderPedidos() {
        const container = document.getElementById('customer-pedidos-list');
        
        if (this.pedidos.length === 0) {
            container.innerHTML = '<p class="text-gray-500">Nenhum pedido encontrado.</p>';
            return;
        }

        const pedidosOrdenados = [...this.pedidos].sort((a, b) => 
            new Date(b.dataPedido) - new Date(a.dataPedido)
        );

        container.innerHTML = `
            <div class="space-y-3 max-h-96 overflow-y-auto">
                ${pedidosOrdenados.slice(0, 5).map(pedido => {
                    const valor = new Intl.NumberFormat('pt-BR', { 
                        style: 'currency', 
                        currency: 'BRL' 
                    }).format(pedido.valorTotal);

                    const data = new Date(pedido.dataPedido).toLocaleDateString('pt-BR');
                    const eta = pedido.etaEntrega ? new Date(pedido.etaEntrega).toLocaleDateString('pt-BR') : 'A definir';

                    return `
                        <div class="border rounded-lg p-4">
                            <div class="flex justify-between items-start">
                                <div>
                                    <h4 class="font-semibold">Pedido #${pedido.numero || pedido.id.substring(0, 8).toUpperCase()}</h4>
                                    <p class="text-sm text-gray-600">${data}</p>
                                    <p class="text-sm text-gray-600">Entrega: ${eta}</p>
                                    <p class="font-bold text-green-600">${valor}</p>
                                </div>
                                <span class="status-${pedido.status.toLowerCase()}">${pedido.status}</span>
                            </div>
                        </div>
                    `;
                }).join('')}
            </div>
            ${this.pedidos.length > 5 ? '<p class="text-sm text-gray-500 mt-2">Mostrando últimos 5 pedidos...</p>' : ''}
        `;
    }
    
    showToast(message, type = 'info') {
        const existing = document.querySelector('.toast');
        if (existing) {
            existing.remove();
        }
        
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        
        const iconMap = {
            'success': '✅',
            'error': '❌',
            'warning': '⚠️',
            'info': 'ℹ️'
        };
        
        const icon = iconMap[type] || iconMap['info'];
        toast.innerHTML = `<span style="margin-right: 8px;">${icon}</span><span>${message}</span>`;
        document.body.appendChild(toast);
        
        // Auto-dismiss after 3 seconds
        setTimeout(() => {
            if (toast.parentNode) {
                toast.style.animation = 'slideOut 0.3s ease-out';
                setTimeout(() => {
                    if (toast.parentNode) {
                        toast.remove();
                    }
                }, 300);
            }
        }, 3000);
    }

    async updateCartDisplay() {
        try {
            // Use data from the loaded cart
            const cartCount = this.carrinho.totalQuantidade || this.carrinho.totalItens || 0;
            const cartTotal = this.carrinho.totalValor || this.carrinho.valorTotal || 0;

            // Update cart display elements if they exist
            const cartCountElement = document.getElementById('cart-count');
            const cartTotalElement = document.getElementById('cart-total');
            
            if (cartCountElement) {
                cartCountElement.textContent = cartCount.count || 0;
            }
            
            if (cartTotalElement) {
                const formatted = new Intl.NumberFormat('pt-BR', { 
                    style: 'currency', 
                    currency: 'BRL' 
                }).format(cartTotal.total || 0);
                cartTotalElement.textContent = formatted;
            }
            
        } catch (error) {
            console.error('Error updating cart display:', error);
        }
    }
}

// Global customer manager instance
window.customerManager = new CustomerManager();