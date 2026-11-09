import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import type { AuthResponse, Owner } from './types';
import { clearSession, getCurrentOwner, getStoredOwner, getStoredToken, loginOwner, registerOwner, storeSession } from './api/client';

interface AuthContextValue {
  owner: Owner | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [owner, setOwner] = useState<Owner | null>(() => getStoredOwner());
  const [loading, setLoading] = useState(() => getStoredToken() !== null);

  useEffect(() => {
    if (!getStoredToken()) {
      setLoading(false);
      return;
    }
    getCurrentOwner()
      .then(setOwner)
      .catch(() => {
        clearSession();
        setOwner(null);
      })
      .finally(() => setLoading(false));
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    owner,
    loading,
    async login(email: string, password: string) {
      const response: AuthResponse = await loginOwner(email, password);
      storeSession(response);
      setOwner(response.owner);
    },
    async register(email: string, password: string, displayName: string) {
      const response: AuthResponse = await registerOwner(email, password, displayName);
      storeSession(response);
      setOwner(response.owner);
    },
    logout() {
      clearSession();
      setOwner(null);
    },
  }), [owner, loading]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return context;
}
