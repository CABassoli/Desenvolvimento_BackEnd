import type { LoginResponse, User } from '@/types/api';
import apiClient from './api-client';

const TOKEN_KEY = 'auth_token';
const USER_KEY = 'auth_user';

export class AuthManager {
  private static instance: AuthManager;
  private token: string | null = null;
  private user: User | null = null;
  private listeners: Set<(user: User | null) => void> = new Set();

  private constructor() {
    this.loadFromStorage();
  }

  static getInstance(): AuthManager {
    if (!AuthManager.instance) {
      AuthManager.instance = new AuthManager();
    }
    return AuthManager.instance;
  }

  private loadFromStorage() {
    if (typeof window === 'undefined') return;

    const storedToken = sessionStorage.getItem(TOKEN_KEY);
    const storedUser = sessionStorage.getItem(USER_KEY);

    if (storedToken && storedUser) {
      try {
        this.token = storedToken;
        this.user = JSON.parse(storedUser);
        apiClient.setToken(storedToken);
      } catch (error) {
        console.error('Erro ao carregar dados de autenticação:', error);
        this.clearAuth();
      }
    }
  }

  private saveToStorage() {
    if (typeof window === 'undefined') return;

    if (this.token && this.user) {
      sessionStorage.setItem(TOKEN_KEY, this.token);
      sessionStorage.setItem(USER_KEY, JSON.stringify(this.user));
    } else {
      sessionStorage.removeItem(TOKEN_KEY);
      sessionStorage.removeItem(USER_KEY);
    }
  }

  private notifyListeners() {
    this.listeners.forEach(listener => listener(this.user));
  }

  async login(email: string, password: string): Promise<LoginResponse> {
    const response = await apiClient.login(email, password) as LoginResponse;
    
    this.token = response.token;
    this.user = {
      id: '', // O backend não retorna ID no login, mas podemos buscar depois
      email: response.email,
      role: response.role,
      isActive: true,
      createdAt: new Date().toISOString(),
    };

    apiClient.setToken(this.token);
    this.saveToStorage();
    this.notifyListeners();

    return response;
  }

  async register(email: string, password: string, role: 'CUSTOMER' | 'MANAGER'): Promise<LoginResponse> {
    await apiClient.register(email, password, role);
    
    // Após registro, fazer login automaticamente
    return this.login(email, password);
  }

  async loadProfile(): Promise<User> {
    if (!this.token) {
      throw new Error('Usuário não autenticado');
    }

    const profile = await apiClient.getProfile() as User;
    this.user = profile;
    this.saveToStorage();
    this.notifyListeners();

    return profile;
  }

  logout() {
    this.token = null;
    this.user = null;
    apiClient.setToken(null);
    this.saveToStorage();
    this.notifyListeners();
  }

  clearAuth() {
    this.logout();
  }

  getToken(): string | null {
    return this.token;
  }

  getUser(): User | null {
    return this.user;
  }

  isAuthenticated(): boolean {
    return this.token !== null && this.user !== null;
  }

  hasRole(role: string): boolean {
    return this.user?.role === role;
  }

  isManager(): boolean {
    return this.hasRole('MANAGER');
  }

  isCustomer(): boolean {
    return this.hasRole('CUSTOMER');
  }

  subscribe(listener: (user: User | null) => void): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }
}

export const authManager = AuthManager.getInstance();
export default authManager;