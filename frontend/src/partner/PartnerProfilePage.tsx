import { useEffect, useState, type FormEvent } from 'react';
import { ApiError, getStoredPartnerToken, updateProfile } from '../api/client';
import { getPartnerProfileV2, updatePartnerProfileV2 } from '../api/partner';
import { usePartnerAuth } from '../partnerAuth';
import { Avatar, Badge, Button, Card, ErrorMessage, NumberInput, PhoneInput, SectionHeader, SelectInput, TextArea, TextInput, UrlInput } from '../components/ui';
import { isValidHttpUrl, isValidPhone, normalizeUrl, safeHref } from '../lib/validation';

const CATEGORIES = ['VET', 'GROOMING', 'HOTEL', 'CAFE', 'PET_SHOP', 'TRAINING', 'WALKER', 'OTHER'] as const;

export function PartnerProfilePage() {
  const { partner, refreshProfile } = usePartnerAuth();
  const [displayName, setDisplayName] = useState('');
  const [businessName, setBusinessName] = useState('');
  const [website, setWebsite] = useState('');
  const [partnerDescription, setPartnerDescription] = useState('');
  const [bio, setBio] = useState('');
  const [phone, setPhone] = useState('');
  const [avatarUrl, setAvatarUrl] = useState('');
  const [latitude, setLatitude] = useState('');
  const [longitude, setLongitude] = useState('');
  const [category, setCategory] = useState<string>('OTHER');
  const [address, setAddress] = useState('');
  const [openingHours, setOpeningHours] = useState('');
  const [coverUrl, setCoverUrl] = useState('');
  const [verified, setVerified] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (!partner) {
      return;
    }
    setDisplayName(partner.displayName);
    setBio(partner.bio ?? '');
    setPhone(partner.phone ?? '');
    setAvatarUrl(partner.avatarUrl ?? '');
  }, [partner]);

  useEffect(() => {
    getPartnerProfileV2()
      .then((p) => {
        setBusinessName(p.businessName || '');
        setCategory(p.category ?? 'OTHER');
        setPartnerDescription(p.description ?? '');
        setWebsite(p.website ?? '');
        setPhone((ph) => p.phone ?? ph);
        setAddress(p.address ?? '');
        setLatitude((la) => p.latitude?.toString() ?? la);
        setLongitude((lo) => p.longitude?.toString() ?? lo);
        setOpeningHours(p.openingHours ?? '');
        setCoverUrl(p.coverUrl ?? '');
        setVerified(Boolean(p.verified));
      })
      .catch(() => {});
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const errors: Record<string, string> = {};
    if (!displayName.trim()) {
      errors.displayName = 'Contact name is required';
    }
    if (!businessName.trim()) {
      errors.businessName = 'Business name is required';
    }
    if (website.trim() && !isValidHttpUrl(website)) {
      errors.website = 'Enter a valid http(s) website URL';
    }
    if (phone.trim() && !isValidPhone(phone)) {
      errors.phone = 'Enter a valid phone number with area code';
    }
    if (avatarUrl.trim() && !isValidHttpUrl(avatarUrl)) {
      errors.avatarUrl = 'Enter a valid http(s) URL';
    }
    if (latitude.trim() && (Number(latitude) < -90 || Number(latitude) > 90)) {
      errors.latitude = 'Latitude must be between -90 and 90';
    }
    if (longitude.trim() && (Number(longitude) < -180 || Number(longitude) > 180)) {
      errors.longitude = 'Longitude must be between -180 and 180';
    }
    if ((latitude.trim() === '') !== (longitude.trim() === '')) {
      errors.coordinates = 'Enter both latitude and longitude or leave both blank';
    }
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }

    setError(null);
    setSaved(false);
    setSubmitting(true);
    try {
      await updateProfile({
        displayName: displayName.trim(),
        bio: bio.trim(),
        phone: phone.trim(),
        avatarUrl: normalizeUrl(avatarUrl),
      }, getStoredPartnerToken());
      await updatePartnerProfileV2({
        businessName: businessName.trim(),
        category: category as NonNullable<Parameters<typeof updatePartnerProfileV2>[0]['category']>,
        description: partnerDescription.trim(),
        website: normalizeUrl(website),
        phone: phone.trim(),
        address: address.trim(),
        latitude: latitude.trim() ? Number(latitude) : undefined,
        longitude: longitude.trim() ? Number(longitude) : undefined,
        clearLocation: !latitude.trim() && !longitude.trim() ? true : undefined,
        openingHours: openingHours.trim(),
        coverUrl: normalizeUrl(coverUrl),
      });
      await refreshProfile();
      setSaved(true);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to update profile');
    } finally {
      setSubmitting(false);
    }
  }

  if (!partner) {
    return null;
  }

  return (
    <div className="space-y-6">
      <SectionHeader title="Partner profile" subtitle="Manage your business profile and contact details." />
      <div className="grid gap-6 lg:grid-cols-[360px_1fr]">
        <Card>
          <div className="flex flex-col items-center text-center">
            <Avatar name={businessName || partner.displayName} src={avatarUrl || partner.avatarUrl} className="h-24 w-24 text-2xl" />
            <h2 className="mt-4 text-xl font-bold text-ink">{businessName || partner.displayName}</h2>
            <p className="text-body text-muted">{partner.email}</p>
            <div className="mt-3 flex gap-2"><Badge tone="accent">Partner</Badge>{verified ? <Badge tone="success">✓ Verified business</Badge> : null}</div>
            {safeHref(website) ? <a className="mt-3 text-body font-semibold text-teal hover:text-teal-dark" href={safeHref(website)!} target="_blank" rel="noreferrer">Visit website</a> : null}
          </div>
        </Card>

        <Card title="Business details" description="This information helps campaigns and recommendations stay trustworthy.">
          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            <div className="grid gap-4 sm:grid-cols-2">
              <TextInput
                label="Contact name"
                required
                value={displayName}
                onChange={(event) => setDisplayName(event.target.value)}
                error={fieldErrors.displayName}
                maxLength={120}
              />
              <TextInput
                label="Business name"
                required
                value={businessName}
                onChange={(event) => setBusinessName(event.target.value)}
                error={fieldErrors.businessName}
                maxLength={160}
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <SelectInput label="Business category" value={category} onChange={(event) => setCategory(event.target.value)}>
                {CATEGORIES.map((c) => <option key={c} value={c}>{c.charAt(0) + c.slice(1).toLowerCase().replace('_', ' ')}</option>)}
              </SelectInput>
              <UrlInput
                label="Website"
                value={website}
                onChange={(event) => setWebsite(event.target.value)}
                error={fieldErrors.website}
                maxLength={300}
                placeholder="https://…"
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <TextInput label="Address" value={address} onChange={(event) => setAddress(event.target.value)} maxLength={300} placeholder="Street, number, neighborhood" />
              <TextInput label="Opening hours" value={openingHours} onChange={(event) => setOpeningHours(event.target.value)} maxLength={300} placeholder="Mon–Sat · 9:00–19:00" />
            </div>
            <UrlInput label="Cover image URL" value={coverUrl} onChange={(event) => setCoverUrl(event.target.value)} maxLength={500} placeholder="https://…" hint="Shown on your map pin and business listing." />
            <div className="grid gap-4 sm:grid-cols-2">
              <NumberInput label="Map latitude" value={latitude} onChange={(event) => setLatitude(event.target.value)} error={fieldErrors.latitude} step="0.000001" placeholder="Optional" />
              <NumberInput label="Map longitude" value={longitude} onChange={(event) => setLongitude(event.target.value)} error={fieldErrors.longitude} step="0.000001" placeholder="Optional" />
            </div>
            <TextArea label="Business description" value={partnerDescription} onChange={(event) => setPartnerDescription(event.target.value)} rows={4} maxLength={1000} />
            <TextArea label="Bio" value={bio} onChange={(event) => setBio(event.target.value)} rows={3} maxLength={500} />
            <div className="grid gap-4 sm:grid-cols-2">
              <PhoneInput
                label="Phone"
                value={phone}
                onChange={(event) => setPhone(event.target.value)}
                error={fieldErrors.phone}
                placeholder="(11) 99999-9999"
              />
              <UrlInput
                label="Avatar URL"
                value={avatarUrl}
                onChange={(event) => setAvatarUrl(event.target.value)}
                error={fieldErrors.avatarUrl}
                maxLength={500}
                placeholder="https://…"
              />
            </div>
            {fieldErrors.coordinates ? <ErrorMessage message={fieldErrors.coordinates} /> : null}
            <ErrorMessage message={error} />
            {saved ? <p className="rounded-2xl bg-success-soft px-4 py-3 text-body text-success">Profile updated.</p> : null}
            <Button type="submit" disabled={submitting}>{submitting ? 'Saving…' : 'Save profile'}</Button>
          </form>
        </Card>
      </div>
    </div>
  );
}
