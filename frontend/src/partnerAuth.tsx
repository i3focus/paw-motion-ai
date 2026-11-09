import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import type { AuthResponse, Owner } from './types';
import {
  clearPartnerSession,
  getCurrentPartner,
  getStoredPartner,
  getStoredPartnerToken,
  loginPartner,
  registerPartner,
  storePartnerSession,
} from './api/client';

interface PartnerAuthContextValue {
  partner: Owner | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (input: {
    email: string;
    password: string;
    displayName: string;
    businessName: string;
    website?: string;
    description?: string;
  }) => Promise<void>;
  refreshProfile: () => Promise<void>;
  logout: () => void;
}

const PartnerAuthContext = createContext<PartnerAuthContextValue | null>(null);

export function PartnerAuthProvider({ children }: { children: ReactNode }) {
  const [partner, setPartner] = useState<Owner | null>(() => getStoredPartner());
  const [loading, setLoading] = useState(() => getStoredPartnerToken() !== null);

  useEffect(() => {
    if (!getStoredPartnerToken()) {
      setLoading(false);
      return;
    }
    getCurrentPartner()
      .then(setPartner)
      .catch(() => {
        clearPartnerSession();
        setPartner(null);
      })
      .finally(() => setLoading(false));
  }, []);

  const value = useMemo<PartnerAuthContextValue>(() => ({
    partner,
    loading,
    async login(email: string, password: string) {
      const response: AuthResponse = await loginPartner(email, password);
      storePartnerSession(response);
      setPartner(response.owner);
    },
    async register(input) {
      const response: AuthResponse = await registerPartner(input);
      storePartnerSession(response);
      setPartner(response.owner);
    },
    async refreshProfile() {
      setPartner(await getCurrentPartner());
    },
    logout() {
      clearPartnerSession();
      setPartner(null);
    },
  }), [partner, loading]);

  return <PartnerAuthContext.Provider value={value}>{children}</PartnerAuthContext.Provider>;
}

export function usePartnerAuth(): PartnerAuthContextValue {
  const context = useContext(PartnerAuthContext);
  if (!context) {
    throw new Error('usePartnerAuth must be used inside PartnerAuthProvider');
  }
  return context;
}
