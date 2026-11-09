import type {
  Activity,
  AuthResponse,
  Campaign,
  DashboardSummary,
  Insight,
  Owner,
  OwnerRecommendation,
  Pet,
} from '../types';

const OWNER_TOKEN_KEY = 'pawmotion.owner.accessToken';
const OWNER_KEY = 'pawmotion.owner.profile';
const PARTNER_TOKEN_KEY = 'pawmotion.partner.accessToken';
const PARTNER_KEY = 'pawmotion.partner.profile';
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export function getStoredOwnerToken(): string | null {
  return localStorage.getItem(OWNER_TOKEN_KEY);
}

export function getStoredOwner(): Owner | null {
  return readProfile(OWNER_KEY);
}

export function getStoredPartnerToken(): string | null {
  return localStorage.getItem(PARTNER_TOKEN_KEY);
}

export function getStoredPartner(): Owner | null {
  return readProfile(PARTNER_KEY);
}

export function storeOwnerSession(auth: AuthResponse): void {
  localStorage.setItem(OWNER_TOKEN_KEY, auth.accessToken);
  localStorage.setItem(OWNER_KEY, JSON.stringify(auth.owner));
}

export function storePartnerSession(auth: AuthResponse): void {
  localStorage.setItem(PARTNER_TOKEN_KEY, auth.accessToken);
  localStorage.setItem(PARTNER_KEY, JSON.stringify(auth.owner));
}

export function clearOwnerSession(): void {
  localStorage.removeItem(OWNER_TOKEN_KEY);
  localStorage.removeItem(OWNER_KEY);
}

export function clearPartnerSession(): void {
  localStorage.removeItem(PARTNER_TOKEN_KEY);
  localStorage.removeItem(PARTNER_KEY);
}

function readProfile(key: string): Owner | null {
  const value = localStorage.getItem(key);
  if (!value) {
    return null;
  }
  try {
    return JSON.parse(value) as Owner;
  } catch {
    localStorage.removeItem(key);
    return null;
  }
}

async function request<T>(path: string, init: RequestInit = {}, token: string | null = getStoredOwnerToken()): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init.headers,
    },
  });

  if (!response.ok) {
    const contentType = response.headers.get('content-type') ?? '';
    let message = `Request failed with status ${response.status}`;
    if (contentType.includes('application/problem+json') || contentType.includes('application/json')) {
      const body = (await response.json()) as { detail?: string; title?: string };
      message = body.detail ?? body.title ?? message;
    }
    throw new ApiError(response.status, message);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export function registerOwner(email: string, password: string, displayName: string): Promise<AuthResponse> {
  return request<AuthResponse>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify({ email, password, displayName }),
  }, null);
}

export function loginOwner(email: string, password: string): Promise<AuthResponse> {
  return request<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  }, null);
}

export function getCurrentOwner(): Promise<Owner> {
  return request<Owner>('/api/me');
}

export function getCurrentPartner(): Promise<Owner> {
  return request<Owner>('/api/me', {}, getStoredPartnerToken());
}

export function updateProfile(input: {
  displayName?: string;
  bio?: string;
  phone?: string;
  avatarUrl?: string;
  businessName?: string;
  website?: string;
  partnerDescription?: string;
}, token: string | null = getStoredOwnerToken()): Promise<Owner> {
  return request<Owner>('/api/me', {
    method: 'PATCH',
    body: JSON.stringify(input),
  }, token);
}

export function listPets(): Promise<Pet[]> {
  return request<Pet[]>('/api/pets');
}

export function createPet(input: {
  name: string;
  species: string;
  breed?: string;
  birthDate?: string;
  size: string;
  activityPreference?: string;
}): Promise<Pet> {
  return request<Pet>('/api/pets', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function listActivities(): Promise<Activity[]> {
  return request<Activity[]>('/api/activities');
}

export function createActivity(input: {
  petId: string;
  type: string;
  startedAt: string;
  durationSeconds: number;
  distanceMeters?: number;
  notes?: string;
}): Promise<Activity> {
  return request<Activity>('/api/activities', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function getDashboardSummary(): Promise<DashboardSummary> {
  return request<DashboardSummary>('/api/dashboard/summary');
}

export function generateInsight(input: { petId?: string; periodDays?: number } = {}): Promise<Insight> {
  return request<Insight>('/api/insights', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function listRecommendations(): Promise<OwnerRecommendation[]> {
  return request<OwnerRecommendation[]>('/api/recommendations');
}

export function registerPartner(input: {
  email: string;
  password: string;
  displayName: string;
  businessName: string;
  website?: string;
  description?: string;
}): Promise<AuthResponse> {
  return request<AuthResponse>('/api/partner/auth/register', {
    method: 'POST',
    body: JSON.stringify(input),
  }, null);
}

export function loginPartner(email: string, password: string): Promise<AuthResponse> {
  return request<AuthResponse>('/api/partner/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  }, null);
}

export function listCampaigns(): Promise<Campaign[]> {
  return request<Campaign[]>('/api/partner/campaigns', {}, getStoredPartnerToken());
}

export function createCampaign(input: {
  title: string;
  description: string;
  targetAudience: string;
  callToAction: string;
}): Promise<Campaign> {
  return request<Campaign>('/api/partner/campaigns', {
    method: 'POST',
    body: JSON.stringify(input),
  }, getStoredPartnerToken());
}

export function analyzeCampaign(campaignId: string): Promise<Campaign> {
  return request<Campaign>(`/api/partner/campaigns/${campaignId}/analyze`, {
    method: 'POST',
  }, getStoredPartnerToken());
}
