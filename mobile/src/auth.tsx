import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import type { Owner } from './types';
import { clearSession, getCurrentOwner, getStoredOwner, getStoredToken, loginOwner, registerOwner, storeSession } from './api/client';

interface AuthContextValue {
  owner: Owner | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName: string) => Promise<void>;
  refreshProfile: () => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [owner, setOwner] = useState<Owner | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    async function bootstrap() {
      const [token, storedOwner] = await Promise.all([getStoredToken(), getStoredOwner()]);
      if (!token) {
        if (!cancelled) {
          setOwner(null);
          setLoading(false);
        }
        return;
      }
      try {
        const current = await getCurrentOwner();
        if (!cancelled) {
          setOwner(current ?? storedOwner);
        }
      } catch {
        await clearSession();
        if (!cancelled) {
          setOwner(null);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }
    bootstrap();
    return () => {
      cancelled = true;
    };
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    owner,
    loading,
    async login(email: string, password: string) {
      const response = await loginOwner(email, password);
      await storeSession(response);
      setOwner(response.owner);
    },
    async register(email: string, password: string, displayName: string) {
      const response = await registerOwner(email, password, displayName);
      await storeSession(response);
      setOwner(response.owner);
    },
    async refreshProfile() {
      setOwner(await getCurrentOwner());
    },
    async logout() {
      await clearSession();
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
