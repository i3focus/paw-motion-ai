/**
 * PETLIFE API client: generated OpenAPI types (see schema.d.ts) plus a minimal fetch helper.
 * Regenerate with `npm run generate` (re-exports the spec and rebuilds the types).
 */

export type { paths, operations, components } from './schema';
import type { components } from './schema';

export type Schemas = components['schemas'];
export type Schema<K extends keyof Schemas> = Schemas[K];

export interface RequestOptions {
    method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
    body?: unknown;
    query?: Record<string, string | number | boolean | undefined>;
    token?: string;
    idempotencyKey?: string;
}

export interface ApiProblem {
    type?: string;
    title?: string;
    status?: number;
    detail?: string;
    instance?: string;
}

export class ApiError extends Error {
    constructor(public status: number, public problem: ApiProblem) {
        super(problem.detail ?? problem.title ?? `HTTP ${status}`);
        this.name = 'ApiError';
    }
}

/** Minimal fetch wrapper shared by web and Expo (both ship a global fetch). */
export async function apiFetch<T>(baseUrl: string, path: string, options: RequestOptions = {}): Promise<T> {
    const url = new URL(path, baseUrl);
    for (const [key, value] of Object.entries(options.query ?? {})) {
        if (value !== undefined) url.searchParams.set(key, String(value));
    }
    const headers: Record<string, string> = {};
    if (options.body !== undefined) headers['Content-Type'] = 'application/json';
    if (options.token) headers.Authorization = `Bearer ${options.token}`;
    if (options.idempotencyKey) headers['Idempotency-Key'] = options.idempotencyKey;

    const response = await fetch(url, {
        method: options.method ?? 'GET',
        headers,
        body: options.body === undefined ? undefined : JSON.stringify(options.body),
    });
    if (response.status === 204) return undefined as T;
    const text = await response.text();
    const parsed: unknown = text ? JSON.parse(text) : undefined;
    if (!response.ok) {
        throw new ApiError(response.status, (parsed ?? {}) as ApiProblem);
    }
    return parsed as T;
}

/** Binds a base URL + token provider once so call sites stay terse. */
export function createClient(baseUrl: string, getToken: () => string | undefined) {
    return <T>(path: string, options: Omit<RequestOptions, 'token'> = {}) =>
        apiFetch<T>(baseUrl, path, { ...options, token: getToken() });
}
