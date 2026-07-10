import { createContext, useContext, useState, useCallback, ReactNode } from 'react';
import { authApi } from '@/api/auth';
import { tokenStorage } from '@/api/client';
import type { Role } from '@/types/api';

interface AuthUser {
  email: string;
  role: Role;
}

interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (payload: { email: string; password: string; firstName: string; lastName: string }) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function readInitialUser(): AuthUser | null {
  const email = tokenStorage.getEmail();
  const role = tokenStorage.getRole();
  if (email && role) return { email, role };
  return null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(readInitialUser);

  const login = useCallback(async (email: string, password: string) => {
    const res = await authApi.login({ email, password });
    tokenStorage.setSession(res.accessToken, res.refreshToken, res.email, res.role);
    setUser({ email: res.email, role: res.role });
  }, []);

  const register = useCallback(
    async (payload: { email: string; password: string; firstName: string; lastName: string }) => {
      const res = await authApi.register(payload);
      tokenStorage.setSession(res.accessToken, res.refreshToken, res.email, res.role);
      setUser({ email: res.email, role: res.role });
    },
    []
  );

  const logout = useCallback(async () => {
    const refreshToken = tokenStorage.getRefreshToken();
    try {
      if (refreshToken) await authApi.logout(refreshToken);
    } catch {
      // ignorujemy - i tak czyscimy sesje lokalnie
    }
    tokenStorage.clear();
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth musi byc uzywany wewnatrz AuthProvider');
  return ctx;
}
