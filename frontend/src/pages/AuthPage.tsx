import { useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import { useAuth } from '../auth';
import { BrandLogo, Button, ErrorMessage, Field, TextInput } from '../components/ui';

export function AuthPage() {
  const { login, register } = useAuth();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
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
        await register(email, password, displayName);
      }
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to authenticate');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-gradient-to-br from-cream-50 via-cream-100 to-accent/20 px-4">
      <div className="w-full max-w-md rounded-3xl border border-cream-200 bg-white p-8 shadow-[0_30px_80px_-40px_rgba(54,39,20,0.5)]">
        <BrandLogo className="text-sm uppercase tracking-wide" />
        <h1 className="mt-2 font-display text-2xl font-bold text-cream-900">Welcome back</h1>
        <p className="mt-1 text-sm text-cream-600">Track activity, progress, and wellness with your pet.</p>

        <div className="mt-6 grid grid-cols-2 rounded-2xl bg-cream-100 p-1 text-sm font-semibold">
          <button type="button" onClick={() => setMode('login')} className={`rounded-xl py-2 transition ${mode === 'login' ? 'bg-white shadow-sm text-cream-900' : 'text-cream-600'}`}>Login</button>
          <button type="button" onClick={() => setMode('register')} className={`rounded-xl py-2 transition ${mode === 'register' ? 'bg-white shadow-sm text-cream-900' : 'text-cream-600'}`}>Register</button>
        </div>

        <form onSubmit={handleSubmit} className="mt-6 space-y-4">
          {mode === 'register' ? (
            <Field label="Display name"><TextInput value={displayName} onChange={(event) => setDisplayName(event.target.value)} required /></Field>
          ) : null}
          <Field label="Email"><TextInput type="email" value={email} onChange={(event) => setEmail(event.target.value)} required /></Field>
          <Field label="Password"><TextInput type="password" value={password} onChange={(event) => setPassword(event.target.value)} required minLength={8} /></Field>
          <ErrorMessage message={error} />
          <Button type="submit" disabled={submitting} className="w-full">{submitting ? 'Please wait…' : mode === 'login' ? 'Login' : 'Create account'}</Button>
        </form>

        <p className="mt-6 text-center text-sm text-cream-600">
          Are you a partner? <a className="font-semibold text-primary hover:text-secondary" href="/partner">Open Partner Portal</a>
        </p>
      </div>
    </main>
  );
}
