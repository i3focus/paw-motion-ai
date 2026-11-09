import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react';

export function BrandLogo({ className = '' }: { className?: string }) {
  return (
    <span className={`font-display font-bold text-secondary ${className}`}>
      PawMotion <span className="text-accent">AI</span>
    </span>
  );
}

export function Avatar({ name, src, className = '' }: { name: string; src?: string | null; className?: string }) {
  const initials = name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('') || 'PM';

  if (src) {
    return <img src={src} alt={name} className={`h-12 w-12 rounded-full border border-cream-200 object-cover ${className}`} />;
  }

  return (
    <span className={`inline-flex h-12 w-12 items-center justify-center rounded-full bg-gradient-to-br from-primary to-accent font-display text-sm font-bold text-white ${className}`}>
      {initials}
    </span>
  );
}

export function Badge({ children, tone = 'neutral' }: { children: ReactNode; tone?: 'neutral' | 'accent' | 'success' | 'warning' | 'danger' }) {
  const tones = {
    neutral: 'bg-cream-100 text-cream-700',
    accent: 'bg-accent-soft text-secondary',
    success: 'bg-emerald-50 text-emerald-700',
    warning: 'bg-amber-50 text-amber-700',
    danger: 'bg-red-50 text-red-700',
  };
  return <span className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold ${tones[tone]}`}>{children}</span>;
}

export function Card({ title, children, className = '' }: { title?: string; children: ReactNode; className?: string }) {
  return (
    <section className={`rounded-3xl border border-cream-200 bg-white p-5 shadow-[0_24px_60px_-35px_rgba(54,39,20,0.45)] ${className}`}>
      {title ? <h2 className="mb-4 text-lg font-semibold text-cream-900">{title}</h2> : null}
      {children}
    </section>
  );
}

export function StatCard({ label, value, hint }: { label: string; value: string | number; hint?: string }) {
  return (
    <div className="rounded-3xl border border-cream-200 bg-white p-4 shadow-[0_24px_60px_-35px_rgba(54,39,20,0.4)]">
      <p className="text-sm font-medium text-cream-500">{label}</p>
      <p className="mt-2 text-3xl font-bold text-cream-900">{value}</p>
      {hint ? <p className="mt-1 text-xs text-cream-500">{hint}</p> : null}
    </div>
  );
}

export function SectionHeader({ title, subtitle, action }: { title: string; subtitle?: string; action?: ReactNode }) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 className="font-display text-2xl font-bold text-cream-900 sm:text-3xl">{title}</h1>
        {subtitle ? <p className="mt-1 text-sm text-cream-600">{subtitle}</p> : null}
      </div>
      {action}
    </div>
  );
}

export function ListRow({ leading, title, subtitle, trailing }: { leading?: ReactNode; title: ReactNode; subtitle?: ReactNode; trailing?: ReactNode }) {
  return (
    <div className="flex items-center gap-4 rounded-2xl border border-cream-200 bg-cream-50 px-4 py-3">
      {leading}
      <div className="min-w-0 flex-1">
        <div className="font-medium text-cream-900">{title}</div>
        {subtitle ? <div className="mt-1 text-sm text-cream-500">{subtitle}</div> : null}
      </div>
      {trailing ? <div className="shrink-0 text-right">{trailing}</div> : null}
    </div>
  );
}

export function ErrorMessage({ message }: { message: string | null }) {
  if (!message) {
    return null;
  }
  return <p className="rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-700">{message}</p>;
}

export function EmptyState({ title, description }: { title: string; description: string }) {
  return (
    <div className="rounded-3xl border border-dashed border-cream-300 bg-cream-50 p-6 text-center">
      <h3 className="font-semibold text-cream-900">{title}</h3>
      <p className="mt-1 text-sm text-cream-600">{description}</p>
    </div>
  );
}

type ButtonVariant = 'primary' | 'secondary' | 'ghost';

export function Button({ variant = 'primary', className = '', ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: ButtonVariant }) {
  const variants = {
    primary: 'bg-primary text-white hover:bg-secondary',
    secondary: 'border border-cream-300 bg-white text-cream-800 hover:bg-cream-50',
    ghost: 'text-cream-700 hover:bg-cream-100',
  };
  return (
    <button
      {...props}
      className={`rounded-2xl px-4 py-2.5 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-60 ${variants[variant]} ${className}`}
    />
  );
}

const fieldClass = 'mt-1 w-full rounded-2xl border border-cream-300 bg-white px-3 py-2.5 text-sm text-cream-900 outline-none transition focus:border-primary focus:ring-2 focus:ring-accent/30';

export function Field({ label, children }: { label: string; children: ReactNode }) {
  return <label className="block text-sm font-medium text-cream-700">{label}{children}</label>;
}

export function TextInput(props: InputHTMLAttributes<HTMLInputElement>) {
  return <input {...props} className={`${fieldClass} ${props.className ?? ''}`} />;
}

export function SelectInput(props: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select {...props} className={`${fieldClass} ${props.className ?? ''}`} />;
}

export function TextArea(props: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea {...props} className={`${fieldClass} ${props.className ?? ''}`} />;
}
