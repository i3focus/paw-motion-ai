import { useEffect, useState, type FormEvent } from 'react';
import { ApiError, getStoredPartnerToken, updateProfile } from '../api/client';
import { usePartnerAuth } from '../partnerAuth';
import { Avatar, Badge, Button, Card, ErrorMessage, Field, SectionHeader, TextArea, TextInput } from '../components/ui';

export function PartnerProfilePage() {
  const { partner, refreshProfile } = usePartnerAuth();
  const [displayName, setDisplayName] = useState('');
  const [businessName, setBusinessName] = useState('');
  const [website, setWebsite] = useState('');
  const [partnerDescription, setPartnerDescription] = useState('');
  const [bio, setBio] = useState('');
  const [phone, setPhone] = useState('');
  const [avatarUrl, setAvatarUrl] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!partner) {
      return;
    }
    setDisplayName(partner.displayName);
    setBusinessName(partner.businessName ?? '');
    setWebsite(partner.website ?? '');
    setPartnerDescription(partner.partnerDescription ?? '');
    setBio(partner.bio ?? '');
    setPhone(partner.phone ?? '');
    setAvatarUrl(partner.avatarUrl ?? '');
  }, [partner]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSaved(false);
    setSubmitting(true);
    try {
      await updateProfile({
        displayName,
        businessName,
        website,
        partnerDescription,
        bio,
        phone,
        avatarUrl,
      }, getStoredPartnerToken());
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
            <h2 className="mt-4 text-xl font-bold text-cream-900">{businessName || partner.displayName}</h2>
            <p className="text-sm text-cream-500">{partner.email}</p>
            <div className="mt-3"><Badge tone="accent">Partner</Badge></div>
            {website ? <a className="mt-3 text-sm font-semibold text-primary hover:text-secondary" href={website} target="_blank" rel="noreferrer">Visit website</a> : null}
          </div>
        </Card>

        <Card title="Business details">
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Contact name"><TextInput value={displayName} onChange={(event) => setDisplayName(event.target.value)} required /></Field>
              <Field label="Business name"><TextInput value={businessName} onChange={(event) => setBusinessName(event.target.value)} required /></Field>
            </div>
            <Field label="Website"><TextInput type="url" value={website} onChange={(event) => setWebsite(event.target.value)} maxLength={300} placeholder="https://…" /></Field>
            <Field label="Business description"><TextArea value={partnerDescription} onChange={(event) => setPartnerDescription(event.target.value)} rows={4} maxLength={1000} /></Field>
            <Field label="Bio"><TextArea value={bio} onChange={(event) => setBio(event.target.value)} rows={3} maxLength={500} /></Field>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Phone"><TextInput value={phone} onChange={(event) => setPhone(event.target.value)} maxLength={40} /></Field>
              <Field label="Avatar URL"><TextInput value={avatarUrl} onChange={(event) => setAvatarUrl(event.target.value)} maxLength={500} placeholder="https://…" /></Field>
            </div>
            <ErrorMessage message={error} />
            {saved ? <p className="rounded-2xl bg-emerald-50 px-4 py-3 text-sm text-emerald-700">Profile updated.</p> : null}
            <Button type="submit" disabled={submitting}>{submitting ? 'Saving…' : 'Save profile'}</Button>
          </form>
        </Card>
      </div>
    </div>
  );
}
