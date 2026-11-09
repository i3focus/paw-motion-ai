import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import type { AuthResponse, Owner } from './types';
import {
  clearOwnerSession,
  getCurrentOwner,
  getStoredOwner,
  getStoredOwnerToken,
  loginOwner,
  registerOwner,
  storeOwnerSession,
} from './api/client';

interface AuthContextValue {
  owner: Owner | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName: string) => Promise<void>;
  refreshProfile: () => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [owner, setOwner] = useState<Owner | null>(() => getStoredOwner());
  const [loading, setLoading] = useState(() => getStoredOwnerToken() !== null);

  useEffect(() => {
    if (!getStoredOwnerToken()) {
      setLoading(false);
      return;
    }
    getCurrentOwner()
      .then(setOwner)
      .catch(() => {
        clearOwnerSession();
        setOwner(null);
      })
      .finally(() => setLoading(false));
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    owner,
    loading,
    async login(email: string, password: string) {
      const response: AuthResponse = await loginOwner(email, password);
      storeOwnerSession(response);
      setOwner(response.owner);
    },
    async register(email: string, password: string, displayName: string) {
      const response: AuthResponse = await registerOwner(email, password, displayName);
      storeOwnerSession(response);
      setOwner(response.owner);
    },
    async refreshProfile() {
      setOwner(await getCurrentOwner());
    },
    logout() {
      clearOwnerSession();
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
