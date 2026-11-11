import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ApiError } from '../api/client';
import { useAuth } from '../auth';
import { BrandLogo, Button, EmailInput, ErrorMessage, PasswordInput, TextInput } from '../components/ui';
import { isValidEmail, normalizeEmail } from '../lib/validation';

const FEATURES = [
  { emoji: '📸', title: 'Feed', description: 'Turn walks and moments into posts, reactions and stories.' },
  { emoji: '🏅', title: 'Adventures', description: 'Earn XP, keep streaks and level up with your pet.' },
  { emoji: '✨', title: 'PetLife AI', description: 'Plans, captions and bookings — executed only with your OK.' },
];

export function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const isLogin = mode === 'login';
  const { login, register } = useAuth();
  const navigate = useNavigate();
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
    if (!isValidEmail(normalizedEmail)) errors.email = 'Enter a valid email address';
    if (password.length < 8) errors.password = 'Password must be at least 8 characters';
    if (!isLogin && displayName.trim().length < 2) errors.displayName = 'Enter your name';
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    setSubmitting(true);
    setError(null);
    try {
      if (isLogin) await login(normalizedEmail, password);
      else await register(normalizedEmail, password, displayName.trim());
      navigate('/app/feed', { replace: true });
    } catch (e) {
      setError(e instanceof ApiError && e.status === 401
        ? 'Incorrect email or password'
        : e instanceof Error ? e.message : 'Something went wrong. Try again.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-screen bg-cream">
      <aside className="hidden w-[42%] flex-col justify-between bg-teal p-10 text-white lg:flex">
        <Link to="/" aria-label="PETLIFE home"><BrandLogo className="text-white [&_svg]:text-white" /></Link>
        <div>
          <h1 className="text-display font-black leading-tight">The social network for pets</h1>
          <div className="mt-8 space-y-4">
            {FEATURES.map((f) => (
              <div key={f.title} className="flex gap-3">
                <span className="text-2xl" aria-hidden="true">{f.emoji}</span>
                <div><p className="font-extrabold">{f.title}</p><p className="text-body text-white/80">{f.description}</p></div>
              </div>
            ))}
          </div>
        </div>
        <p className="text-label text-white/60">Free to start · Web and mobile</p>
      </aside>

      <main className="flex flex-1 items-center justify-center p-6">
        <div className="w-full max-w-md">
          <Link to="/" className="mb-8 inline-block lg:hidden"><BrandLogo /></Link>
          <h2 className="text-heading">{isLogin ? 'Welcome back' : 'Create your account'}</h2>
          <p className="mt-1 text-body text-muted">
            {isLogin ? 'Sign in to keep up with your pack.' : "Your pet's profile is a minute away."}
          </p>

          <form onSubmit={handleSubmit} className="mt-6 space-y-4" noValidate>
            {!isLogin ? (
              <TextInput label="Your name" value={displayName} onChange={(e) => setDisplayName(e.target.value)}
                error={fieldErrors.displayName} autoComplete="name" required />
            ) : null}
            <EmailInput label="Email" value={email} onChange={(e) => setEmail(e.target.value)} error={fieldErrors.email} required />
            <PasswordInput label="Password" value={password} onChange={(e) => setPassword(e.target.value)}
              error={fieldErrors.password} autoComplete={isLogin ? 'current-password' : 'new-password'}
              hint={isLogin ? undefined : 'At least 8 characters'} required />
            <ErrorMessage message={error} />
            <Button type="submit" className="w-full" size="lg" disabled={submitting}>
              {submitting ? 'Please wait…' : isLogin ? 'Sign in' : 'Create account'}
            </Button>
          </form>

          <p className="mt-6 text-center text-body text-muted">
            {isLogin ? (
              <>New to PetLife? <Link to="/signup" className="font-extrabold text-teal hover:underline">Create a free profile</Link></>
            ) : (
              <>Already have an account? <Link to="/login" className="font-extrabold text-teal hover:underline">Sign in</Link></>
            )}
          </p>
          <p className="mt-3 text-center text-caption text-muted">
            Have a pet business? <Link to="/partner" className="font-bold text-teal hover:underline">Partner portal</Link>
          </p>
        </div>
      </main>
    </div>
  );
}
