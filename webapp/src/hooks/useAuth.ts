import { useState, useEffect } from 'react';
import type { User } from '@/types/api';
import { authManager } from '@/lib/auth';

export function useAuth() {
  const [user, setUser] = useState<User | null>(authManager.getUser());
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    const unsubscribe = authManager.subscribe(setUser);
    return unsubscribe;
  }, []);

  const login = async (email: string, password: string) => {
    setIsLoading(true);
    try {
      const response = await authManager.login(email, password);
      return response;
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (email: string, password: string, role: 'CUSTOMER' | 'MANAGER') => {
    setIsLoading(true);
    try {
      const response = await authManager.register(email, password, role);
      return response;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    authManager.logout();
  };

  return {
    user,
    isAuthenticated: authManager.isAuthenticated(),
    isManager: authManager.isManager(),
    isCustomer: authManager.isCustomer(),
    isLoading,
    login,
    register,
    logout,
  };
}