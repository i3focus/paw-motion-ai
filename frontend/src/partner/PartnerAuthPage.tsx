import { useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import { usePartnerAuth } from '../partnerAuth';
import { BrandLogo, Button, EmailInput, ErrorMessage, PasswordInput, TextArea, TextInput, UrlInput } from '../components/ui';
import { isValidEmail, isValidHttpUrl, normalizeEmail, normalizeUrl } from '../lib/validation';

export function PartnerAuthPage() {
  const { login, register } = usePartnerAuth();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [businessName, setBusinessName] = useState('');
  const [website, setWebsite] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const errors: Record<string, string> = {};
    const normalizedEmail = normalizeEmail(email);
    if (!isValidEmail(normalizedEmail)) {
      errors.email = 'Enter a valid email address';
    }
    if (password.length < 8) {
      errors.password = 'Password must be at least 8 characters';
    }
    if (mode === 'register') {
      if (!displayName.trim()) {
        errors.displayName = 'Contact name is required';
      }
      if (!businessName.trim()) {
        errors.businessName = 'Business name is required';
      }
      if (website.trim() && !isValidHttpUrl(website)) {
        errors.website = 'Enter a valid http(s) website URL';
      }
    }
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }

    setError(null);
    setSubmitting(true);
    try {
      if (mode === 'login') {
        await login(normalizedEmail, password);
      } else {
        await register({
          email: normalizedEmail,
          password,
          displayName: displayName.trim(),
          businessName: businessName.trim(),
          website: normalizeUrl(website) || undefined,
          description: description.trim() || undefined,
        });
      }
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to authenticate');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-gradient-to-br from-cream-50 via-cream-100 to-accent/20 px-4 py-10">
      <div className="w-full max-w-lg rounded-3xl border border-cream-200 bg-white p-8 shadow-[0_30px_80px_-40px_rgba(54,39,20,0.5)]">
        <BrandLogo className="text-sm uppercase tracking-wide" />
        <h1 className="mt-2 font-display text-2xl font-bold text-cream-900">Partner Portal</h1>
        <p className="mt-1 text-sm text-cream-600">Register your business, publish campaigns, and let AI recommend topics to pet owners.</p>

        <div className="mt-6 grid grid-cols-2 rounded-2xl bg-cream-100 p-1 text-sm font-semibold">
          <button type="button" onClick={() => setMode('login')} className={`rounded-xl py-2 transition ${mode === 'login' ? 'bg-white shadow-sm text-cream-900' : 'text-cream-600'}`}>Login</button>
          <button type="button" onClick={() => setMode('register')} className={`rounded-xl py-2 transition ${mode === 'register' ? 'bg-white shadow-sm text-cream-900' : 'text-cream-600'}`}>Register</button>
        </div>

        <form onSubmit={handleSubmit} className="mt-6 space-y-4" noValidate>
          {mode === 'register' ? (
            <>
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
              <UrlInput
                label="Website"
                value={website}
                onChange={(event) => setWebsite(event.target.value)}
                error={fieldErrors.website}
                maxLength={300}
                placeholder="https://…"
              />
              <TextArea label="Business description" value={description} onChange={(event) => setDescription(event.target.value)} rows={3} maxLength={1000} />
            </>
          ) : null}
          <EmailInput
            label="Email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            error={fieldErrors.email}
          />
          <PasswordInput
            label="Password"
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            error={fieldErrors.password}
            minLength={8}
            autoComplete={mode === 'register' ? 'new-password' : 'current-password'}
          />
          <ErrorMessage message={error} />
          <Button type="submit" disabled={submitting} className="w-full">{submitting ? 'Please wait…' : mode === 'login' ? 'Login' : 'Register partner'}</Button>
        </form>

        <p className="mt-6 text-center text-sm text-cream-600">
          Pet owner? <a className="font-semibold text-primary hover:text-secondary" href="/">Open owner portal</a>
        </p>
      </div>
    </main>
  );
}
