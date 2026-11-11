import { isValidEmail, isValidHttpUrl, isValidPhone, isPositiveInteger, normalizeEmail, normalizeUrl } from '../validation';

describe('isValidEmail', () => {
  it('accepts a normal email and rejects malformed input', () => {
    expect(isValidEmail('ana@petlife.app')).toBe(true);
    expect(isValidEmail('not-an-email')).toBe(false);
  });
});

describe('isValidHttpUrl', () => {
  it('accepts http(s) URLs and bare domains, rejects malformed input', () => {
    expect(isValidHttpUrl('https://petlife.app')).toBe(true);
    expect(isValidHttpUrl('petlife.app/shop')).toBe(true);
    expect(isValidHttpUrl('https://has space.com')).toBe(false);
    expect(isValidHttpUrl('https://')).toBe(false);
    expect(isValidHttpUrl('')).toBe(true);
  });
});

describe('normalizeUrl / normalizeEmail', () => {
  it('prepends https when missing and lowercases emails', () => {
    expect(normalizeUrl('petlife.app')).toBe('https://petlife.app');
    expect(normalizeUrl('http://x.co')).toBe('http://x.co');
    expect(normalizeEmail('  Ana@Example.COM ')).toBe('ana@example.com');
  });
});

describe('isValidPhone / isPositiveInteger', () => {
  it('validates phone and integer inputs', () => {
    expect(isValidPhone('(11) 99999-9999')).toBe(true);
    expect(isValidPhone('123')).toBe(false);
    expect(isPositiveInteger('12')).toBe(true);
    expect(isPositiveInteger('-3')).toBe(false);
    expect(isPositiveInteger('1.5')).toBe(false);
  });
});
