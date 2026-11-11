import { useEffect, useState, type FormEvent } from 'react';
import { ApiError, updateProfile } from '../api/client';
import { useAuth } from '../auth';
import { Avatar, Badge, Button, Card, ErrorMessage, PhoneInput, SectionHeader, TextArea, TextInput, UrlInput } from '../components/ui';
import { isValidHttpUrl, isValidPhone, normalizeUrl } from '../lib/validation';

export function ProfilePage() {
  const { owner, refreshProfile } = useAuth();
  const [displayName, setDisplayName] = useState('');
  const [bio, setBio] = useState('');
  const [phone, setPhone] = useState('');
  const [avatarUrl, setAvatarUrl] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

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
    const errors: Record<string, string> = {};
    if (!displayName.trim()) {
      errors.displayName = 'Display name is required';
    }
    if (phone.trim() && !isValidPhone(phone)) {
      errors.phone = 'Enter a valid phone number with area code';
    }
    if (avatarUrl.trim() && !isValidHttpUrl(avatarUrl)) {
      errors.avatarUrl = 'Enter a valid http(s) URL';
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
      <SectionHeader title="Trainer profile" subtitle="Manage your pet-owner account details." />
      <div className="grid gap-6 lg:grid-cols-[360px_1fr]">
        <Card>
          <div className="flex flex-col items-center text-center">
            <Avatar name={displayName || owner.displayName} src={avatarUrl || owner.avatarUrl} className="h-24 w-24 text-2xl" />
            <h2 className="mt-4 text-xl font-bold text-cream-900">{displayName || owner.displayName}</h2>
            <p className="text-sm text-cream-500">{owner.email}</p>
            <div className="mt-3"><Badge tone="accent">Pet owner</Badge></div>
          </div>
        </Card>

        <Card title="Account details" description="Keep your contact details current for pet activity updates.">
          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            <TextInput
              label="Display name"
              required
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
              error={fieldErrors.displayName}
              maxLength={120}
            />
            <TextArea label="Bio" value={bio} onChange={(event) => setBio(event.target.value)} rows={4} maxLength={500} placeholder="Tell us about you and your pets" />
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
            <ErrorMessage message={error} />
            {saved ? <p className="rounded-2xl bg-emerald-50 px-4 py-3 text-sm text-emerald-700">Profile updated.</p> : null}
            <Button type="submit" disabled={submitting}>{submitting ? 'Saving…' : 'Save profile'}</Button>
          </form>
        </Card>
      </div>
    </div>
  );
}
