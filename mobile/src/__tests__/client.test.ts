jest.mock('@react-native-async-storage/async-storage', () =>
  require('@react-native-async-storage/async-storage/jest/async-storage-mock'));

import { ApiError, request } from '../api/client';

const fetchMock = jest.fn();
globalThis.fetch = fetchMock as unknown as typeof fetch;

function jsonResponse(status: number, body: unknown = {}, headers: Record<string, string> = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json', ...headers },
  });
}

describe('request', () => {
  beforeEach(() => {
    fetchMock.mockReset();
  });

  it('returns undefined for 204 responses instead of parsing a body', async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await expect(request<undefined>('/api/ping', {}, null)).resolves.toBeUndefined();
  });

  it('parses JSON bodies on success', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(200, { ok: true }));
    await expect(request<{ ok: boolean }>('/api/ping', {}, null)).resolves.toEqual({ ok: true });
  });

  it('surfaces problem detail messages on errors', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(422, { detail: 'Validation failed' }));
    await expect(request('/api/ping', {}, null)).rejects.toMatchObject({ status: 422, message: 'Validation failed' });
  });

  it('falls back to a status message for non-JSON errors', async () => {
    fetchMock.mockResolvedValueOnce(new Response('oops', { status: 500 }));
    await expect(request('/api/ping', {}, null)).rejects.toMatchObject({ status: 500, name: 'ApiError' });
  });
});
