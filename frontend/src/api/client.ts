import type {
  Activity,
  AuthResponse,
  DashboardSummary,
  Insight,
  Owner,
  Pet,
} from '../types';

const TOKEN_KEY = 'pawmotion.accessToken';
const OWNER_KEY = 'pawmotion.owner';
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

export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function getStoredOwner(): Owner | null {
  const value = localStorage.getItem(OWNER_KEY);
  if (!value) {
    return null;
  }
  return JSON.parse(value) as Owner;
}

export function storeSession(auth: AuthResponse): void {
  localStorage.setItem(TOKEN_KEY, auth.accessToken);
  localStorage.setItem(OWNER_KEY, JSON.stringify(auth.owner));
}

export function clearSession(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(OWNER_KEY);
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getStoredToken();
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
  });
}

export function loginOwner(email: string, password: string): Promise<AuthResponse> {
  return request<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
}

export function getCurrentOwner(): Promise<Owner> {
  return request<Owner>('/api/me');
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
