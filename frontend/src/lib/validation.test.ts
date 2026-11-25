import { describe, expect, it } from 'vitest';
import { isStrongPassword, isValidHttpUrl, normalizeUrl, passwordIssues, safeHref } from './validation';

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
    expect(safeHref('https://')).toBeNull();
    expect(safeHref('http:///x')).toBeNull();
  });
});

describe('passwordIssues / isStrongPassword', () => {
  it('accepts a strong password', () => {
    expect(isStrongPassword('PetLife123!')).toBe(true);
    expect(passwordIssues('PetLife123!')).toEqual([]);
  });

  it('rejects weak passwords and reports the missing rules', () => {
    expect(isStrongPassword('petlife')).toBe(false);
    expect(passwordIssues('petlife1')).toEqual(['an uppercase letter', 'a symbol']);
    expect(passwordIssues('short')).toContain('at least 8 characters');
    expect(passwordIssues('PASSWORD1')).toEqual(['a lowercase letter', 'a symbol']);
    expect(passwordIssues('Password')).toEqual(['a number', 'a symbol']);
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
