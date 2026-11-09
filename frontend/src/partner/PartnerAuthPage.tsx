import { useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import { usePartnerAuth } from '../partnerAuth';
import { BrandLogo, Button, ErrorMessage, Field, TextArea, TextInput } from '../components/ui';

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

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      if (mode === 'login') {
        await login(email, password);
      } else {
        await register({
          email,
          password,
          displayName,
          businessName,
          website: website || undefined,
          description: description || undefined,
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

        <form onSubmit={handleSubmit} className="mt-6 space-y-4">
          {mode === 'register' ? (
            <>
              <Field label="Contact name"><TextInput value={displayName} onChange={(event) => setDisplayName(event.target.value)} required /></Field>
              <Field label="Business name"><TextInput value={businessName} onChange={(event) => setBusinessName(event.target.value)} required /></Field>
              <Field label="Website"><TextInput type="url" value={website} onChange={(event) => setWebsite(event.target.value)} placeholder="https://…" /></Field>
              <Field label="Business description"><TextArea value={description} onChange={(event) => setDescription(event.target.value)} rows={3} maxLength={1000} /></Field>
            </>
          ) : null}
          <Field label="Email"><TextInput type="email" value={email} onChange={(event) => setEmail(event.target.value)} required /></Field>
          <Field label="Password"><TextInput type="password" value={password} onChange={(event) => setPassword(event.target.value)} required minLength={8} /></Field>
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
