import AsyncStorage from '@react-native-async-storage/async-storage';
import type { Activity, AuthResponse, DashboardSummary, Insight, Owner, OwnerRecommendation, Pet } from '../types';

const TOKEN_KEY = 'pawmotion.mobile.accessToken';
const OWNER_KEY = 'pawmotion.mobile.owner';
const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';

export class ApiError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message);
    this.name = 'ApiError';
  }
}

export async function getStoredToken(): Promise<string | null> {
  return AsyncStorage.getItem(TOKEN_KEY);
}

export async function getStoredOwner(): Promise<Owner | null> {
  const value = await AsyncStorage.getItem(OWNER_KEY);
  if (!value) {
    return null;
  }
  try {
    return JSON.parse(value) as Owner;
  } catch {
    await AsyncStorage.removeItem(OWNER_KEY);
    return null;
  }
}

export async function storeSession(auth: AuthResponse): Promise<void> {
  await AsyncStorage.setItem(TOKEN_KEY, auth.accessToken);
  await AsyncStorage.setItem(OWNER_KEY, JSON.stringify(auth.owner));
}

export async function clearSession(): Promise<void> {
  await AsyncStorage.removeItem(TOKEN_KEY);
  await AsyncStorage.removeItem(OWNER_KEY);
}

async function request<T>(path: string, init: RequestInit = {}, token?: string | null): Promise<T> {
  const authToken = token === undefined ? await getStoredToken() : token;
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
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

export function updateProfile(input: {
  displayName?: string;
  bio?: string;
  phone?: string;
  avatarUrl?: string;
}): Promise<Owner> {
  return request<Owner>('/api/me', {
    method: 'PATCH',
    body: JSON.stringify(input),
  });
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
