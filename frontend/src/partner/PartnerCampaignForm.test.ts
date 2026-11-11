import { describe, expect, it } from 'vitest';
import { draftErrors, draftFromCampaign, draftToUpdate, type CampaignDraft } from './PartnerCampaignForm';

function validDraft(overrides: Partial<CampaignDraft> = {}): CampaignDraft {
  return {
    ...draftFromCampaign(),
    title: 'Spring groom',
    description: 'Trim for the season',
    targetAudience: 'dog owners',
    ctaLabel: 'Book now',
    ctaUrl: 'https://petlife.app/book',
    ...overrides,
  };
}

describe('draftErrors', () => {
  it('accepts a complete draft', () => {
    expect(draftErrors(validDraft())).toEqual({});
  });

  it('requires an http(s) destination for LINK CTAs', () => {
    expect(draftErrors(validDraft({ ctaUrl: '' })).ctaUrl).toBeTruthy();
    expect(draftErrors(validDraft({ ctaUrl: 'javascript:x' })).ctaUrl).toBeTruthy();
    expect(draftErrors(validDraft({ ctaUrl: 'https://ok.app' })).ctaUrl).toBeUndefined();
  });

  it('requires a bound service for BOOKING CTAs', () => {
    expect(draftErrors(validDraft({ ctaType: 'BOOKING', serviceId: '' })).serviceId).toBeTruthy();
    expect(draftErrors(validDraft({ ctaType: 'BOOKING', serviceId: 's1' })).serviceId).toBeUndefined();
  });

  it('rejects inverted date ranges and missing formats', () => {
    const past = validDraft();
    past.endsAt = '2020-01-01T00:00';
    expect(draftErrors(past).endsAt).toBeTruthy();
    expect(draftErrors(validDraft({ formats: [] })).formats).toBeTruthy();
  });

  it('validates target coordinates only when both are present', () => {
    expect(draftErrors(validDraft({ targetLat: '10', targetLng: '' })).targetLat).toBeTruthy();
    expect(draftErrors(validDraft({ targetLat: '91', targetLng: '0' })).targetLat).toBeTruthy();
    expect(draftErrors(validDraft({ targetLat: '10', targetLng: '20', targetRadiusKm: '5' })).targetLat).toBeUndefined();
  });
});

describe('draftToUpdate', () => {
  it('omits ctaUrl for BOOKING CTAs and ctaType service for LINK', () => {
    const booking = draftToUpdate(validDraft({ ctaType: 'BOOKING', serviceId: 'svc-1', ctaUrl: 'https://x.co' }), true);
    expect(booking.serviceId).toBe('svc-1');
    expect(booking.ctaUrl).toBeUndefined();
    const link = draftToUpdate(validDraft({ serviceId: 'svc-1' }), true);
    expect(link.serviceId).toBeUndefined();
    expect(link.ctaUrl).toBe('https://petlife.app/book');
  });

  it('sets clearTargetLocation when coordinates are blank and maps km to meters', () => {
    expect(draftToUpdate(validDraft(), true).clearTargetLocation).toBe(true);
    const withLoc = draftToUpdate(validDraft({ targetLat: '10.5', targetLng: '-46.6', targetRadiusKm: '2.5' }), true);
    expect(withLoc.targetLatitude).toBe(10.5);
    expect(withLoc.targetRadiusMeters).toBe(2500);
    expect(withLoc.clearTargetLocation).toBeUndefined();
  });

  it('only includes base fields when includeBase is set', () => {
    const update = draftToUpdate(validDraft(), false);
    expect(update.title).toBeUndefined();
    expect(update.formats).toBeDefined();
  });
});
