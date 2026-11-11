import { describe, expect, it } from 'vitest';
import { isValidHttpUrl, normalizeUrl, safeHref } from './validation';

describe('safeHref', () => {
  it('returns http(s) URLs unchanged and rejects every other scheme', () => {
    expect(safeHref('https://petlife.app/x')).toBe('https://petlife.app/x');
    expect(safeHref('http://petlife.app')).toBe('http://petlife.app');
    expect(safeHref('javascript:alert(1)')).toBeNull();
    expect(safeHref('petlife.app/path')).toBeNull();
    expect(safeHref('  ')).toBeNull();
    expect(safeHref(null)).toBeNull();
    expect(safeHref(undefined)).toBeNull();
    expect(safeHref('data:text/html,<b>x</b>')).toBeNull();
  });
});

describe('isValidHttpUrl / normalizeUrl', () => {
  it('accepts http(s) and bare domains', () => {
    expect(isValidHttpUrl('https://petlife.app')).toBe(true);
    expect(isValidHttpUrl('petlife.app')).toBe(true);
    expect(normalizeUrl('petlife.app')).toBe('https://petlife.app');
    expect(normalizeUrl('http://x.co')).toBe('http://x.co');
  });
});
