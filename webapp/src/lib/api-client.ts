import type { ApiError } from '@/types/api';

const API_BASE_URL = '/api';

class ApiClient {
  private baseURL: string;
  private token: string | null = null;

  constructor(baseURL: string = API_BASE_URL) {
    this.baseURL = baseURL;
  }

  setToken(token: string | null) {
    this.token = token;
  }

  getToken(): string | null {
    return this.token;
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${this.baseURL}${endpoint}`;
    
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(options.headers as Record<string, string>),
    };

    if (this.token) {
      headers['Authorization'] = `Bearer ${this.token}`;
    }

    const config: RequestInit = {
      ...options,
      headers,
    };

    try {
      const response = await fetch(url, config);
      
      if (!response.ok) {
        const errorData = await response.text();
        let error: ApiError;
        
        try {
          const parsed = JSON.parse(errorData);
          error = {
            message: parsed.message || 'Erro na requisição',
            code: response.status.toString(),
            details: parsed.details,
          };
        } catch {
          error = {
            message: errorData || 'Erro na requisição',
            code: response.status.toString(),
          };
        }

        // Token expirado ou inválido
        if (response.status === 401) {
          this.handleUnauthorized();
        }

        throw error;
      }

      const contentType = response.headers.get('content-type');
      if (contentType && contentType.includes('application/json')) {
        return await response.json();
      }
      
      return {} as T;
    } catch (error) {
      if (error instanceof TypeError && error.message.includes('fetch')) {
        throw {
          message: 'Erro de conexão com o servidor',
          code: 'NETWORK_ERROR',
        } as ApiError;
      }
      throw error;
    }
  }

  private handleUnauthorized() {
    this.token = null;
    // Remove token do sessionStorage
    sessionStorage.removeItem('auth_token');
    // Redireciona para login
    window.location.href = '/login';
  }

  // Métodos HTTP
  async get<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: 'GET' });
  }

  async post<T>(endpoint: string, data?: unknown): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'POST',
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  async put<T>(endpoint: string, data?: unknown): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PUT',
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  async delete<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: 'DELETE' });
  }

  // Métodos de autenticação
  async login(email: string, password: string) {
    return this.post('/auth/login', { email, password });
  }

  async register(email: string, password: string, role: string) {
    return this.post('/auth/register', { email, password, role });
  }

  async getProfile() {
    return this.get('/auth/profile');
  }

  // Health check
  async healthCheck() {
    return this.get('/health');
  }
}

export const apiClient = new ApiClient();
export default apiClient;