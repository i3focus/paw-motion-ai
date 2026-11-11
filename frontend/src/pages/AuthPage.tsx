import { useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import { useAuth } from '../auth';
import { PawIcon } from '../components/petVisuals';
import { BrandLogo, Button, EmailInput, ErrorMessage, PawMark, PasswordInput, TextInput } from '../components/ui';
import { isValidEmail, normalizeEmail } from '../lib/validation';

const FEATURES = [
  { title: 'Feed', description: 'Turn activities into posts, reactions, comments, and map moments.' },
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
    <main className="bg-cream relative min-h-screen overflow-hidden lg:grid lg:grid-cols-[1.05fr_1fr]">

      <section className="relative hidden overflow-hidden bg-teal-dark p-10 text-white lg:flex lg:flex-col lg:justify-between">
        <div className="pl-map-grid absolute inset-0 opacity-20" />
        <div className="relative inline-flex w-fit rounded-2xl bg-white/95 px-4 py-3 shadow-xl">
          <BrandLogo className="text-xl" />
        </div>
        <div className="relative max-w-xl">
          <p className="text-body font-bold uppercase tracking-[0.24em] text-mint">PETLIFE is open</p>
          <h1 className="mt-4 font-display text-5xl font-bold leading-[1.02] tracking-tight text-white">Your pet's city is waiting.</h1>
          <p className="mt-5 max-w-lg text-base leading-7 text-white/85">
            Record real activity, publish city posts, discover partners, and turn daily walks into a game your pack will love.
          </p>
          <div className="mt-8 space-y-4">
            {FEATURES.map((feature) => (
              <div key={feature.title} className="flex items-start gap-3 rounded-3xl border border-white/15 bg-white/10 p-4 backdrop-blur">
                <span className="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-mint text-teal-dark">
                  <PawIcon className="h-6 w-6" />
                </span>
                <div>
                  <p className="font-bold text-white">{feature.title}</p>
                  <p className="mt-1 text-body leading-6 text-white/85">{feature.description}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
        <div className="relative mt-10 hidden max-w-md overflow-hidden rounded-[32px] border border-white/15 shadow-2xl xl:block">
          <img src="/assets/dog-portrait.jpg" alt="Happy dog ready for PETLIFE" className="h-56 w-full object-cover" />
          <div className="absolute inset-0 bg-gradient-to-t from-teal-dark/70 via-transparent to-transparent" />
          <div className="absolute bottom-4 left-4 right-4 flex items-end justify-between gap-3">
            <div>
              <p className="font-display text-2xl font-bold text-white">Ready for the next mission?</p>
              <p className="text-body text-white/85">Login to continue your pack's streak.</p>
            </div>
            <PawMark className="h-12 w-12" />
          </div>
        </div>
      </section>

      <section className="relative flex items-center justify-center px-4 py-10 sm:px-6">
        <div className="pl-card w-full max-w-md p-6 sm:p-8">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="text-label font-bold uppercase tracking-[0.2em] text-muted">Pet owner portal</p>
              <h2 className="mt-2 font-display text-3xl font-bold text-ink">{mode === 'login' ? 'Welcome back' : 'Join PETLIFE'}</h2>
            </div>
            <span className="inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-teal-light text-teal">
              <PawMark className="h-6 w-6" />
            </span>
          </div>
          <p className="mt-2 text-body text-muted">Track activity, progress, and wellness with your pet.</p>

          <div className="mt-6 grid grid-cols-2 rounded-2xl bg-sand p-1 text-body font-semibold">
            <button type="button" onClick={() => setMode('login')} className={`rounded-xl py-2 transition ${mode === 'login' ? 'bg-white shadow-sm text-ink' : 'text-muted'}`}>Login</button>
            <button type="button" onClick={() => setMode('register')} className={`rounded-xl py-2 transition ${mode === 'register' ? 'bg-white shadow-sm text-ink' : 'text-muted'}`}>Register</button>
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

          <p className="mt-6 text-center text-body text-muted">
            Are you a partner? <a className="font-semibold text-teal hover:text-teal-dark" href="/partner">Open Partner Portal</a>
          </p>
        </div>
      </section>
    </main>
  );
}
