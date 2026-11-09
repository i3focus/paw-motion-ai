import { useEffect, useState, type FormEvent } from 'react';
import { ApiError, updateProfile } from '../api/client';
import { useAuth } from '../auth';
import { Avatar, Badge, Button, Card, ErrorMessage, Field, SectionHeader, TextArea, TextInput } from '../components/ui';

export function ProfilePage() {
  const { owner, refreshProfile } = useAuth();
  const [displayName, setDisplayName] = useState('');
  const [bio, setBio] = useState('');
  const [phone, setPhone] = useState('');
  const [avatarUrl, setAvatarUrl] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!owner) {
      return;
    }
    setDisplayName(owner.displayName);
    setBio(owner.bio ?? '');
    setPhone(owner.phone ?? '');
    setAvatarUrl(owner.avatarUrl ?? '');
  }, [owner]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSaved(false);
    setSubmitting(true);
    try {
      await updateProfile({
        displayName,
        bio: bio || undefined,
        phone: phone || undefined,
        avatarUrl: avatarUrl || undefined,
      });
      await refreshProfile();
      setSaved(true);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to update profile');
    } finally {
      setSubmitting(false);
    }
  }

  if (!owner) {
    return null;
  }

  return (
    <div className="space-y-6">
      <SectionHeader title="Profile" subtitle="Manage your pet-owner account details." />
      <div className="grid gap-6 lg:grid-cols-[360px_1fr]">
        <Card>
          <div className="flex flex-col items-center text-center">
            <Avatar name={displayName || owner.displayName} src={avatarUrl || owner.avatarUrl} className="h-24 w-24 text-2xl" />
            <h2 className="mt-4 text-xl font-bold text-cream-900">{displayName || owner.displayName}</h2>
            <p className="text-sm text-cream-500">{owner.email}</p>
            <div className="mt-3"><Badge tone="accent">Pet owner</Badge></div>
          </div>
        </Card>

        <Card title="Account details">
          <form onSubmit={handleSubmit} className="space-y-4">
            <Field label="Display name"><TextInput value={displayName} onChange={(event) => setDisplayName(event.target.value)} required /></Field>
            <Field label="Bio"><TextArea value={bio} onChange={(event) => setBio(event.target.value)} rows={4} maxLength={500} placeholder="Tell us about you and your pets" /></Field>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Phone"><TextInput value={phone} onChange={(event) => setPhone(event.target.value)} maxLength={40} placeholder="+55 11 99999-9999" /></Field>
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
