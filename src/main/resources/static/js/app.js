// Main application initialization
document.addEventListener('DOMContentLoaded', async function() {
    console.log('E-commerce Application JavaScript loaded successfully');
    console.log('E-commerce Application Starting...');
    
    // Guarda de rota para páginas protegidas
    const currentPath = window.location.pathname;
    const protectedPaths = ['/admin.html', '/customer.html', '/admin', '/customer'];
    const isProtected = protectedPaths.some(path => currentPath.includes(path));
    
    if (isProtected) {
        if (!Auth.token) {
            window.location.href = '/';
            return;
        }
        
        // Ping auth/me para verificar token válido
        try {
            const me = await window.api.getMe();
            if (!me || !me.id) {
                Auth.clear();
                window.location.href = '/';
                return;
            }
            Auth.user = me;
        } catch (e) {
            Auth.clear();
            window.location.href = '/';
            return;
        }
    }
    
    // Initialize the application
    initializeApp();
});

async function initializeApp() {
    try {
        // Test API connection
        await window.api.healthCheck();
        console.log('API connection successful');
        
        // Check if user is already logged in
        if (window.authManager.isAuthenticated()) {
            console.log('User already authenticated');
            
            // Verificar se o token ainda é válido
            try {
                const me = await window.api.getMe();
                if (me && me.id) {
                    Auth.user = me;
                    window.authManager.showDashboard();
                } else {
                    throw new Error('Invalid user data');
                }
            } catch (e) {
                console.log('Token expired or invalid, showing login');
                Auth.clear();
                window.authManager.clearAuth();
                window.authManager.showLoginForm();
            }
        } else {
            console.log('User not authenticated, showing login');
            window.authManager.showLoginForm();
        }
        
    } catch (error) {
        console.error('Failed to initialize application:', error);
        showError('Erro ao conectar com o servidor. Verifique sua conexão.');
    }
}

function showError(message) {
    // Create a temporary error message
    const errorDiv = document.createElement('div');
    errorDiv.className = 'fixed top-4 left-1/2 transform -translate-x-1/2 bg-red-500 text-white px-6 py-3 rounded-lg shadow-lg z-50';
    errorDiv.textContent = message;
    
    document.body.appendChild(errorDiv);
    
    // Remove after 5 seconds
    setTimeout(() => {
        document.body.removeChild(errorDiv);
    }, 5000);
}

function showSuccess(message) {
    // Create a temporary success message
    const successDiv = document.createElement('div');
    successDiv.className = 'fixed top-4 left-1/2 transform -translate-x-1/2 bg-green-500 text-white px-6 py-3 rounded-lg shadow-lg z-50';
    successDiv.textContent = message;
    
    document.body.appendChild(successDiv);
    
    // Remove after 3 seconds
    setTimeout(() => {
        document.body.removeChild(successDiv);
    }, 3000);
}

// Utility functions
function formatCurrency(value) {
    return new Intl.NumberFormat('pt-BR', { 
        style: 'currency', 
        currency: 'BRL' 
    }).format(value);
}

function formatDate(dateString) {
    return new Date(dateString).toLocaleDateString('pt-BR');
}

// Global utility functions
window.showError = showError;
window.showSuccess = showSuccess;
window.formatCurrency = formatCurrency;
window.formatDate = formatDate;

console.log('E-commerce Application JavaScript loaded successfully');