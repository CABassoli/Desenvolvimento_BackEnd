// Authentication Store
const Auth = {
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

// Bootstrap after login function
async function bootstrapAfterLogin() {
    try {
        const me = await window.api.getMe();
        if (!me || !me.role) return;
        
        Auth.user = me;
        
        if (me.role === 'MANAGER') {
            window.authManager.showDashboard();
        } else {
            window.authManager.showDashboard();
        }
    } catch (e) {
        console.error('Bootstrap failed:', e);
    }
}

// Authentication management
class AuthManager {
    constructor() {
        this.user = null;
        this.loadUser();
        this.setupEventListeners();
    }

    loadUser() {
        this.user = Auth.user;
    }

    saveUser(user) {
        this.user = user;
        Auth.user = user;
    }

    clearAuth() {
        this.user = null;
        Auth.clear();
        window.api.setToken(null);
    }

    isAuthenticated() {
        return Auth.token !== '';
    }

    isManager() {
        return this.user?.role === 'MANAGER';
    }

    isCustomer() {
        return this.user?.role === 'CUSTOMER';
    }

    setupEventListeners() {
        // Login form
        document.getElementById('login-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.handleLogin();
        });

        // Register form
        document.getElementById('register-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.handleRegister();
        });

        // Show register form
        document.getElementById('show-register').addEventListener('click', () => {
            this.showRegisterForm();
        });

        // Show login form
        document.getElementById('show-login').addEventListener('click', () => {
            this.showLoginForm();
        });

        // Login button
        document.getElementById('login-btn').addEventListener('click', () => {
            this.showLoginForm();
        });

        // Logout button
        document.getElementById('logout-btn').addEventListener('click', () => {
            this.logout();
        });

        // Handle unauthorized event
        window.addEventListener('unauthorized', () => {
            this.logout();
        });
    }

    async handleLogin() {
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;
        const errorDiv = document.getElementById('login-error');

        this.showLoading(true);
        errorDiv.classList.add('hidden');

        try {
            const response = await window.api.login(email, password);
            
            if (response && response.token) {
                Auth.token = response.token;
                window.api.setToken(response.token);
                
                const user = {
                    id: response.user.id,
                    email: response.user.email,
                    role: response.user.role
                };
                
                this.saveUser(user);
                await bootstrapAfterLogin();
            } else {
                throw new Error('Resposta inválida do servidor');
            }
            
        } catch (error) {
            errorDiv.textContent = error.message || 'Erro ao fazer login';
            errorDiv.classList.remove('hidden');
        } finally {
            this.showLoading(false);
        }
    }

    async handleRegister() {
        const email = document.getElementById('reg-email').value;
        const password = document.getElementById('reg-password').value;
        const role = document.getElementById('reg-role').value;
        const errorDiv = document.getElementById('register-error');

        this.showLoading(true);
        errorDiv.classList.add('hidden');

        try {
            await window.api.register(email, password, role);
            
            // After successful registration, try to login
            const loginResponse = await window.api.login(email, password);
            
            if (loginResponse && loginResponse.token) {
                Auth.token = loginResponse.token;
                window.api.setToken(loginResponse.token);
                
                const user = {
                    id: loginResponse.user.id,
                    email: loginResponse.user.email,
                    role: loginResponse.user.role
                };
                
                this.saveUser(user);
                await bootstrapAfterLogin();
            } else {
                throw new Error('Erro ao fazer login após registro');
            }
            
        } catch (error) {
            errorDiv.textContent = error.message;
            errorDiv.classList.remove('hidden');
        } finally {
            this.showLoading(false);
        }
    }

    showLoginForm() {
        document.getElementById('login-section').classList.remove('hidden');
        document.getElementById('register-section').classList.add('hidden');
        document.getElementById('dashboard-section').classList.add('hidden');
        document.getElementById('login-error').classList.add('hidden');
    }

    showRegisterForm() {
        document.getElementById('login-section').classList.add('hidden');
        document.getElementById('register-section').classList.remove('hidden');
        document.getElementById('dashboard-section').classList.add('hidden');
        document.getElementById('register-error').classList.add('hidden');
    }

    showDashboard() {
        document.getElementById('login-section').classList.add('hidden');
        document.getElementById('register-section').classList.add('hidden');
        document.getElementById('dashboard-section').classList.remove('hidden');
        
        // Update UI
        document.getElementById('user-info').textContent = `${this.user.email} (${this.user.role})`;
        document.getElementById('user-info').classList.remove('hidden');
        document.getElementById('login-btn').classList.add('hidden');
        document.getElementById('logout-btn').classList.remove('hidden');
        
        // Update app title based on role
        const appTitle = document.getElementById('app-title');
        if (this.isManager()) {
            appTitle.textContent = '🛒 E-commerce Admin';
            document.getElementById('admin-dashboard').classList.remove('hidden');
            document.getElementById('customer-dashboard').classList.add('hidden');
            window.adminManager.loadDashboard();
        } else {
            appTitle.textContent = '🛒 E-commerce Cliente';
            document.getElementById('admin-dashboard').classList.add('hidden');
            document.getElementById('customer-dashboard').classList.remove('hidden');
            window.customerManager.loadDashboard();
        }
    }

    logout() {
        this.clearAuth();
        window.api.logout();
        document.getElementById('user-info').classList.add('hidden');
        document.getElementById('login-btn').classList.remove('hidden');
        document.getElementById('logout-btn').classList.add('hidden');
        document.getElementById('app-title').textContent = '🛒 E-commerce';
        this.showLoginForm();
    }
    
    getCurrentUser() {
        return Auth.user;
    }

    showLoading(show) {
        if (show) {
            document.getElementById('loading').classList.remove('hidden');
        } else {
            document.getElementById('loading').classList.add('hidden');
        }
    }
}

// Global auth manager instance
window.authManager = new AuthManager();