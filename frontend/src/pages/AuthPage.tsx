import { useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import { useAuth } from '../auth';
import { PawIcon } from '../components/petVisuals';
import { BrandLogo, Button, EmailInput, ErrorMessage, PoppyMark, PasswordInput, TextInput } from '../components/ui';
import { isValidEmail, normalizeEmail } from '../lib/validation';

const FEATURES = [
  { title: 'Paw City feed', description: 'Turn activities into posts, reactions, comments, and map moments.' },
  { title: 'Trainer quests', description: 'Earn XP, keep streaks, and level up with your pack.' },
  { title: 'AI Activity Coach', description: 'Get non-medical guidance from real activity history.' },
];

export function AuthPage() {
  const { login, register } = useAuth();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
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
    if (mode === 'register' && !displayName.trim()) {
      errors.displayName = 'Display name is required';
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
        await register(normalizedEmail, password, displayName.trim());
      }
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to authenticate');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="paw-city-bg relative min-h-screen overflow-hidden lg:grid lg:grid-cols-[1.05fr_1fr]">
      <PoppyMark className="poppy-float pointer-events-none absolute left-[6%] top-[10%] h-20 w-20 text-accent/25 lg:hidden" />
      <PoppyMark className="float-slow pointer-events-none absolute bottom-[8%] right-[6%] h-28 w-28 text-primary/15" />

      <section className="relative hidden overflow-hidden bg-[linear-gradient(135deg,#362714_0%,#5e4424_48%,#825e31_100%)] p-10 text-cream-50 lg:flex lg:flex-col lg:justify-between">
        <div className="city-grid absolute inset-0 opacity-20" />
        <PoppyMark className="float-slow pointer-events-none absolute right-10 top-10 h-28 w-28 text-accent/30" />
        <PoppyMark className="poppy-float pointer-events-none absolute bottom-24 left-12 h-16 w-16 text-white/15" />
        <div className="relative inline-flex w-fit rounded-2xl bg-white/95 px-4 py-3 shadow-xl">
          <BrandLogo className="text-xl" />
        </div>
        <div className="relative max-w-xl">
          <p className="text-sm font-bold uppercase tracking-[0.24em] text-accent">Paw City is open</p>
          <h1 className="mt-4 font-display text-5xl font-bold leading-[1.02] tracking-tight">Your pet's city is waiting.</h1>
          <p className="mt-5 max-w-lg text-base leading-7 text-cream-100/90">
            Record real activity, publish city posts, discover partners, and turn daily walks into a game your pack will love.
          </p>
          <div className="mt-8 space-y-4">
            {FEATURES.map((feature) => (
              <div key={feature.title} className="flex items-start gap-3 rounded-3xl border border-white/15 bg-white/10 p-4 backdrop-blur">
                <span className="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-accent text-primary">
                  <PawIcon className="h-6 w-6" />
                </span>
                <div>
                  <p className="font-bold text-white">{feature.title}</p>
                  <p className="mt-1 text-sm leading-6 text-cream-100/85">{feature.description}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
        <div className="relative mt-10 hidden max-w-md overflow-hidden rounded-[32px] border border-white/15 shadow-2xl xl:block">
          <img src="/assets/dog-portrait.jpg" alt="Happy dog ready for Paw City" className="h-56 w-full object-cover" />
          <div className="absolute inset-0 bg-gradient-to-t from-dark/70 via-transparent to-transparent" />
          <div className="absolute bottom-4 left-4 right-4 flex items-end justify-between gap-3">
            <div>
              <p className="font-display text-2xl font-bold text-white">Ready for the next mission?</p>
              <p className="text-sm text-white/85">Login to continue your pack's streak.</p>
            </div>
            <PoppyMark className="h-12 w-12 text-accent" />
          </div>
        </div>
      </section>

      <section className="relative flex items-center justify-center px-4 py-10 sm:px-6">
        <div className="game-card w-full max-w-md p-6 sm:p-8">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="text-xs font-bold uppercase tracking-[0.2em] text-cream-500">Pet owner portal</p>
              <h2 className="mt-2 font-display text-3xl font-bold text-cream-900">{mode === 'login' ? 'Welcome back' : 'Join Paw City'}</h2>
            </div>
            <span className="inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-accent-soft text-primary">
              <PoppyMark className="h-6 w-6" />
            </span>
          </div>
          <p className="mt-2 text-sm text-cream-600">Track activity, progress, and wellness with your pet.</p>

          <div className="mt-6 grid grid-cols-2 rounded-2xl bg-cream-100 p-1 text-sm font-semibold">
            <button type="button" onClick={() => setMode('login')} className={`rounded-xl py-2 transition ${mode === 'login' ? 'bg-white shadow-sm text-cream-900' : 'text-cream-600'}`}>Login</button>
            <button type="button" onClick={() => setMode('register')} className={`rounded-xl py-2 transition ${mode === 'register' ? 'bg-white shadow-sm text-cream-900' : 'text-cream-600'}`}>Register</button>
          </div>

          <form onSubmit={handleSubmit} className="mt-6 space-y-4" noValidate>
            {mode === 'register' ? (
              <TextInput
                label="Display name"
                required
                value={displayName}
                onChange={(event) => setDisplayName(event.target.value)}
                error={fieldErrors.displayName}
                maxLength={120}
              />
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
            <Button type="submit" disabled={submitting} className="w-full">{submitting ? 'Please wait…' : mode === 'login' ? 'Login' : 'Create account'}</Button>
          </form>

          <p className="mt-6 text-center text-sm text-cream-600">
            Are you a partner? <a className="font-semibold text-primary hover:text-secondary" href="/partner">Open Partner Portal</a>
          </p>
        </div>
      </section>
    </main>
  );
}
