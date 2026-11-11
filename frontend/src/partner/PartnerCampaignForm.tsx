import { useEffect, useMemo, useRef, useState } from 'react';
import { ApiError } from '../api/client';
import {
  listPartnerServices,
  uploadPartnerMedia,
  type CampaignV2,
  type PartnerService,
  type UpdateCampaignRequest,
} from '../api/partner';
import { Badge, Button, Chip, Field, NumberInput, SelectInput, TextArea, TextInput } from '../components/ui';
import { FORMAT_LABEL } from './PartnerCampaignsPage';

const FORMATS = ['FEED_CARD', 'MAP_PIN', 'DISCOVER_HIGHLIGHT', 'NEARBY_PROMO', 'AI_RECOMMENDATION'] as const;

export interface CampaignDraft {
  title: string;
  description: string;
  targetAudience: string;
  ctaLabel: string;
  ctaType: 'LINK' | 'BOOKING';
  ctaUrl: string;
  serviceId: string;
  mediaAssetId: string;
  mediaPreview: string;
  startsAt: string;
  endsAt: string;
  targetLat: string;
  targetLng: string;
  targetRadiusKm: string;
  species: string;
  breeds: string;
  interests: string;
  formats: string[];
}

const toLocal = (iso?: string | null) => {
  if (!iso) return '';
  const d = new Date(iso);
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  return d.toISOString().slice(0, 16);
};

const nowLocal = (offsetMs = 0) => {
  const d = new Date(Date.now() + offsetMs);
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  return d.toISOString().slice(0, 16);
};

export function draftFromCampaign(c?: CampaignV2 | null): CampaignDraft {
  return {
    title: c?.title ?? '',
    description: c?.description ?? '',
    targetAudience: c?.targetAudience ?? '',
    ctaLabel: c?.ctaLabel ?? 'Learn more',
    ctaType: (c?.ctaType as 'LINK' | 'BOOKING') ?? 'LINK',
    ctaUrl: c?.ctaUrl ?? '',
    serviceId: c?.serviceId ?? '',
    mediaAssetId: '',
    mediaPreview: c?.mediaUrl ?? '',
    startsAt: toLocal(c?.startsAt) || nowLocal(),
    endsAt: toLocal(c?.endsAt) || nowLocal(30 * 86_400_000),
    targetLat: c?.targetLocation?.latitude?.toString() ?? '',
    targetLng: c?.targetLocation?.longitude?.toString() ?? '',
    targetRadiusKm: ((c?.targetLocation?.radiusMeters ?? 5000) / 1000).toString(),
    species: (c?.targetSpecies ?? []).join(', '),
    breeds: (c?.targetBreeds ?? []).join(', '),
    interests: (c?.targetInterests ?? []).join(', '),
    formats: c?.formats?.length ? [...c.formats] : ['FEED_CARD'],
  };
}

export function draftErrors(d: CampaignDraft): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!d.title.trim()) errors.title = 'Title is required';
  if (!d.description.trim()) errors.description = 'Description is required';
  if (!d.targetAudience.trim()) errors.targetAudience = 'Target audience is required';
  if (!d.ctaLabel.trim()) errors.ctaLabel = 'CTA label is required';
  if (d.ctaType === 'LINK' && !d.ctaUrl.trim()) errors.ctaUrl = 'A link CTA needs a destination URL';
  if (d.ctaType === 'LINK' && d.ctaUrl.trim() && !/^https?:\/\//i.test(d.ctaUrl.trim())) errors.ctaUrl = 'URL must start with http:// or https://';
  if (d.ctaType === 'BOOKING' && !d.serviceId) errors.serviceId = 'Choose the service to book';
  if (!d.startsAt || !d.endsAt) errors.endsAt = 'Pick a start and end date';
  else if (new Date(d.endsAt) <= new Date(d.startsAt)) errors.endsAt = 'End must be after start';
  if (d.formats.length === 0) errors.formats = 'Pick at least one ad format';
  if ((d.targetLat.trim() === '') !== (d.targetLng.trim() === '')) {
    errors.targetLat = 'Enter both coordinates or neither';
  } else if (d.targetLat.trim() && d.targetLng.trim()) {
    const lat = Number(d.targetLat);
    const lng = Number(d.targetLng);
    const radius = Number(d.targetRadiusKm);
    if (!Number.isFinite(lat) || lat < -90 || lat > 90) errors.targetLat = 'Latitude must be between -90 and 90';
    else if (!Number.isFinite(lng) || lng < -180 || lng > 180) errors.targetLat = 'Longitude must be between -180 and 180';
    else if (!Number.isFinite(radius) || radius < 0.5 || radius > 100) errors.targetLat = 'Radius must be between 0.5 and 100 km';
  }
  return errors;
}

export function draftToUpdate(d: CampaignDraft, includeBase: boolean): UpdateCampaignRequest {
  const csv = (v: string) => v.split(',').map((s) => s.trim()).filter(Boolean);
  const update: UpdateCampaignRequest = {
    ctaType: d.ctaType,
    ctaUrl: d.ctaType === 'LINK' ? d.ctaUrl.trim() : undefined,
    serviceId: d.ctaType === 'BOOKING' ? d.serviceId : undefined,
    mediaAssetId: d.mediaAssetId || undefined,
    startsAt: new Date(d.startsAt).toISOString(),
    endsAt: new Date(d.endsAt).toISOString(),
    targetSpecies: csv(d.species).map((s) => s.toUpperCase()),
    targetBreeds: csv(d.breeds),
    targetInterests: csv(d.interests).map((s) => s.toLowerCase()),
    formats: d.formats as UpdateCampaignRequest['formats'],
  };
  if (includeBase) {
    update.title = d.title.trim();
    update.description = d.description.trim();
    update.targetAudience = d.targetAudience.trim();
    update.ctaLabel = d.ctaLabel.trim();
  }
  if (d.targetLat.trim() && d.targetLng.trim()) {
    update.targetLatitude = Number(d.targetLat);
    update.targetLongitude = Number(d.targetLng);
    update.targetRadiusMeters = Math.round(Number(d.targetRadiusKm || '5') * 1000);
  } else {
    update.clearTargetLocation = true;
  }
  return update;
}

export function CampaignForm({
  draft,
  onChange,
  errors,
  onMedia,
  uploading,
}: {
  draft: CampaignDraft;
  onChange: (d: CampaignDraft) => void;
  errors: Record<string, string>;
  onMedia: (file: File) => void;
  uploading: boolean;
}) {
  const [services, setServices] = useState<PartnerService[]>([]);
  const fileRef = useRef<HTMLInputElement>(null);
  const set = (patch: Partial<CampaignDraft>) => onChange({ ...draft, ...patch });

  useEffect(() => {
    listPartnerServices().then(setServices).catch(() => setServices([]));
  }, []);

  const activeServices = useMemo(() => {
    const list = services.filter((s) => s.active);
    const bound = draft.serviceId ? services.find((s) => s.id === draft.serviceId) : undefined;
    return bound && !list.some((s) => s.id === bound.id) ? [bound, ...list] : list;
  }, [services, draft.serviceId]);

  return (
    <div className="space-y-4">
      <TextInput label="Campaign title" required value={draft.title} onChange={(e) => set({ title: e.target.value })} error={errors.title} maxLength={160} />
      <TextArea label="Description" required value={draft.description} onChange={(e) => set({ description: e.target.value })} error={errors.description} rows={4} maxLength={2000} />
      <TextInput label="Target audience" required value={draft.targetAudience} onChange={(e) => set({ targetAudience: e.target.value })} error={errors.targetAudience} maxLength={160} placeholder="Active dog owners in Pinheiros" />

      <Field label="Ad formats" required error={errors.formats} hint="Where the ad can appear. Sponsored placements are always labeled.">
        <div className="flex flex-wrap gap-2">
          {FORMATS.map((f) => (
            <Chip
              key={f}
              selected={draft.formats.includes(f)}
              onClick={() => set({ formats: draft.formats.includes(f) ? draft.formats.filter((x) => x !== f) : [...draft.formats, f] })}
            >
              {FORMAT_LABEL[f]}
            </Chip>
          ))}
        </div>
      </Field>

      <Field label="Ad media" required hint="Shown on the sponsored card (required before submitting for review).">
        <div className="flex items-center gap-3">
          {draft.mediaPreview ? <img src={draft.mediaPreview} alt="Ad media preview" className="h-20 w-20 rounded-2xl object-cover" /> : null}
          <input ref={fileRef} type="file" accept="image/*" className="hidden"
            onChange={(e) => { const f = e.target.files?.[0]; e.target.value = ''; if (f) onMedia(f); }} />
          <Button type="button" variant="secondary" onClick={() => fileRef.current?.click()} disabled={uploading}>
            {uploading ? 'Uploading…' : draft.mediaPreview ? 'Replace image' : 'Upload image'}
          </Button>
        </div>
      </Field>

      <div className="grid gap-4 sm:grid-cols-2">
        <TextInput label="Starts" required type="datetime-local" value={draft.startsAt} onChange={(e) => set({ startsAt: e.target.value })} />
        <TextInput label="Ends" required type="datetime-local" value={draft.endsAt} onChange={(e) => set({ endsAt: e.target.value })} error={errors.endsAt} />
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <SelectInput label="CTA type" value={draft.ctaType} onChange={(e) => set({ ctaType: e.target.value as 'LINK' | 'BOOKING' })}>
          <option value="LINK">External link</option>
          <option value="BOOKING">Book a service</option>
        </SelectInput>
        <TextInput label="CTA label" required value={draft.ctaLabel} onChange={(e) => set({ ctaLabel: e.target.value })} error={errors.ctaLabel} maxLength={160} />
      </div>
      {draft.ctaType === 'LINK' ? (
        <TextInput label="Destination URL" required type="url" value={draft.ctaUrl} onChange={(e) => set({ ctaUrl: e.target.value })} error={errors.ctaUrl} placeholder="https://…" />
      ) : (
        <SelectInput label="Bookable service" required value={draft.serviceId} onChange={(e) => set({ serviceId: e.target.value })} error={errors.serviceId}>
          <option value="">Choose a service…</option>
          {activeServices.map((s) => <option key={s.id} value={s.id}>{s.name} · {(s.priceCents ?? 0) / 100} {s.currency}{s.active ? '' : ' (paused)'}</option>)}
        </SelectInput>
      )}
      {draft.ctaType === 'BOOKING' && activeServices.length === 0 ? (
        <p className="rounded-2xl bg-amber-soft px-4 py-3 text-label text-amber-ink">
          No active services yet — create one under <Badge tone="warning">Services</Badge> first.
        </p>
      ) : null}

      <Field label="Location targeting" hint="Optional — limit the ad to members near a point." error={errors.targetLat}>
        <div className="grid gap-4 sm:grid-cols-3">
          <NumberInput label="Latitude" value={draft.targetLat} onChange={(e) => set({ targetLat: e.target.value })} step="0.000001" placeholder="-23.56" />
          <NumberInput label="Longitude" value={draft.targetLng} onChange={(e) => set({ targetLng: e.target.value })} step="0.000001" placeholder="-46.68" />
          <NumberInput label="Radius (km)" value={draft.targetRadiusKm} onChange={(e) => set({ targetRadiusKm: e.target.value })} step="0.5" min="0.5" max="100" />
        </div>
      </Field>

      <div className="grid gap-4 sm:grid-cols-3">
        <TextInput label="Species" value={draft.species} onChange={(e) => set({ species: e.target.value })} hint="Comma-separated, e.g. DOG, CAT" />
        <TextInput label="Breeds" value={draft.breeds} onChange={(e) => set({ breeds: e.target.value })} hint="e.g. Golden Retriever" />
        <TextInput label="Interests" value={draft.interests} onChange={(e) => set({ interests: e.target.value })} hint="e.g. hiking, grooming" />
      </div>
    </div>
  );
}

export function useMediaUpload(setDraft: (patch: Partial<CampaignDraft>) => void) {
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const objectUrl = useRef<string | null>(null);

  useEffect(() => () => {
    if (objectUrl.current) URL.revokeObjectURL(objectUrl.current);
  }, []);

  const upload = async (file: File) => {
    setUploading(true);
    setUploadError(null);
    try {
      const media = await uploadPartnerMedia(file);
      if (!media.id) throw new Error('Upload did not return a media id');
      let preview = media.url ?? null;
      if (!preview) {
        if (objectUrl.current) URL.revokeObjectURL(objectUrl.current);
        objectUrl.current = URL.createObjectURL(file);
        preview = objectUrl.current;
      }
      setDraft({ mediaAssetId: media.id, mediaPreview: preview });
    } catch (e) {
      setUploadError(e instanceof ApiError || e instanceof Error ? e.message : 'Upload failed');
    } finally {
      setUploading(false);
    }
  };
  return { upload, uploading, uploadError };
}
