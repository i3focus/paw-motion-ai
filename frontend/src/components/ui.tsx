import { useId, type ButtonHTMLAttributes, type InputHTMLAttributes, type ReactNode, type SVGProps, type SelectHTMLAttributes, type TextareaHTMLAttributes } from 'react';
import { formatPhoneInput } from '../lib/validation';

export function PoppyMark({ className = '', ...props }: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" className={className} fill="currentColor" aria-hidden="true" {...props}>
      <ellipse cx="12" cy="15.5" rx="4.8" ry="3.8" />
      <circle cx="6.2" cy="10.2" r="1.9" />
      <circle cx="10" cy="7.2" r="1.9" />
      <circle cx="14" cy="7.2" r="1.9" />
      <circle cx="17.8" cy="10.2" r="1.9" />
    </svg>
  );
}

export function BrandLogo({ compact = false, className = '' }: { compact?: boolean; className?: string }) {
  return (
    <span className={`inline-flex items-center gap-2 font-display font-bold tracking-tight text-cream-900 ${className || 'text-xl'}`}>
      <span className="inline-flex h-9 w-9 items-center justify-center rounded-2xl bg-gradient-to-br from-primary via-supporting to-accent text-white shadow-[0_14px_30px_-18px_rgba(130,94,49,0.9)]">
        <PoppyMark className="h-5 w-5" />
      </span>
      {!compact ? <span>Paw<span className="text-accent">Motion</span> <span className="text-accent">AI</span></span> : null}
    </span>
  );
}

export function Avatar({ name, imageUrl, src, size = 'md', className = '' }: { name: string; imageUrl?: string | null; src?: string | null; size?: 'sm' | 'md' | 'lg'; className?: string }) {
  const initials = name.split(' ').filter(Boolean).slice(0, 2).map((part) => part[0]).join('').toUpperCase();
  const source = imageUrl ?? src;
  const sizeClass = size === 'lg' ? 'h-16 w-16 text-lg' : size === 'sm' ? 'h-8 w-8 text-xs' : 'h-11 w-11 text-sm';
  const dimensionClass = className || sizeClass;
  if (source) {
    return <img src={source} alt={name} className={`${dimensionClass} rounded-full border border-cream-200 object-cover shadow-sm`} />;
  }
  return <span className={`${dimensionClass} inline-flex items-center justify-center rounded-full bg-gradient-to-br from-primary to-accent font-bold text-white shadow-sm`}>{initials || 'PM'}</span>;
}

export function Badge({ children, tone = 'neutral' }: { children: ReactNode; tone?: 'neutral' | 'success' | 'warning' | 'danger' | 'accent' }) {
  const tones = {
    neutral: 'border-cream-200 bg-cream-100 text-cream-700',
    success: 'border-emerald-200 bg-emerald-50 text-emerald-800',
    warning: 'border-amber-200 bg-amber-50 text-amber-800',
    danger: 'border-red-200 bg-red-50 text-red-700',
    accent: 'border-accent/40 bg-accent-soft text-primary',
  } as const;
  return <span className={`inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-semibold ${tones[tone]}`}>{children}</span>;
}

export function Card({ children, className = '', title, description }: { children: ReactNode; className?: string; title?: string; description?: string }) {
  return (
    <section className={`rounded-[30px] border border-cream-200 bg-white shadow-[0_30px_80px_-45px_rgba(54,39,20,0.55)] ${className}`}>
      {title ? (
        <div className="border-b border-cream-100 px-5 py-4 sm:px-6">
          <h2 className="font-display text-lg font-bold text-cream-900">{title}</h2>
          {description ? <p className="mt-1 text-sm text-cream-500">{description}</p> : null}
        </div>
      ) : null}
      <div className={title ? 'p-5 sm:p-6' : ''}>{children}</div>
    </section>
  );
}

export function StatCard({ label, value, hint }: { label: string; value: ReactNode; hint?: string }) {
  return (
    <Card className="p-5">
      <p className="text-xs font-semibold uppercase tracking-[0.18em] text-cream-500">{label}</p>
      <p className="mt-3 font-display text-4xl font-bold text-cream-900">{value}</p>
      {hint ? <p className="mt-2 text-sm text-cream-500">{hint}</p> : null}
    </Card>
  );
}

export function SectionHeader({ title, description, subtitle, action }: { title: string; description?: string; subtitle?: string; action?: ReactNode }) {
  const text = description ?? subtitle;
  return (
    <div className="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h2 className="font-display text-2xl font-bold text-cream-900">{title}</h2>
        {text ? <p className="mt-1 text-sm text-cream-500">{text}</p> : null}
      </div>
      {action}
    </div>
  );
}

export function ListRow({ leading, title, subtitle, trailing }: { leading?: ReactNode; title: ReactNode; subtitle?: ReactNode; trailing?: ReactNode }) {
  return (
    <div className="flex items-center gap-3 rounded-2xl border border-cream-200 bg-cream-50 p-3">
      {leading}
      <div className="min-w-0 flex-1">
        <p className="truncate font-semibold text-cream-900">{title}</p>
        {subtitle ? <p className="truncate text-sm text-cream-500">{subtitle}</p> : null}
      </div>
      {trailing}
    </div>
  );
}

export function ErrorMessage({ message }: { message?: string | null }) {
  if (!message) {
    return null;
  }
  return <p className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700">{message}</p>;
}

export function EmptyState({ title, description, action }: { title: string; description: string; action?: ReactNode }) {
  return (
    <Card className="flex flex-col items-center justify-center p-8 text-center">
      <div className="inline-flex h-14 w-14 items-center justify-center rounded-3xl bg-gradient-to-br from-accent-soft to-cream-100 text-primary shadow-inner">
        <PoppyMark className="h-7 w-7" />
      </div>
      <h3 className="mt-4 font-display text-xl font-bold text-cream-900">{title}</h3>
      <p className="mt-2 max-w-md text-sm text-cream-500">{description}</p>
      {action ? <div className="mt-5">{action}</div> : null}
    </Card>
  );
}

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';

export function Button({ children, variant = 'primary', className = '', ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: ButtonVariant }) {
  const variants: Record<ButtonVariant, string> = {
    primary: 'bg-primary text-white shadow-sm hover:bg-secondary disabled:bg-cream-300 disabled:text-cream-500',
    secondary: 'border border-cream-300 bg-white text-cream-800 hover:border-accent hover:text-primary disabled:text-cream-400',
    ghost: 'text-primary hover:bg-accent-soft disabled:text-cream-400',
    danger: 'border border-red-200 bg-red-50 text-red-700 hover:bg-red-100 disabled:text-red-300',
  };
  return <button className={`inline-flex min-h-11 items-center justify-center gap-2 rounded-full px-5 py-2.5 text-sm font-semibold transition active:scale-[0.98] disabled:cursor-not-allowed ${variants[variant]} ${className}`} {...props}>{children}</button>;
}

type FieldProps = {
  label: string;
  htmlFor?: string;
  required?: boolean;
  error?: string;
  hint?: string;
  children: ReactNode;
};

export function Field({ label, htmlFor, required, error, hint, children }: FieldProps) {
  const generatedId = useId();
  const id = htmlFor ?? generatedId;
  return (
    <div className="space-y-2">
      <label htmlFor={id} className="block text-sm font-semibold text-cream-800">
        {label}
        {required ? <span className="ml-1 text-accent">*</span> : null}
      </label>
      {children}
      {error ? <p className="text-xs font-medium text-red-600">{error}</p> : hint ? <p className="text-xs text-cream-500">{hint}</p> : null}
    </div>
  );
}

const inputClassName = (error?: string) => `w-full rounded-2xl border ${error ? 'border-red-300 bg-red-50/40' : 'border-cream-300 bg-white'} px-4 py-3 text-sm text-cream-900 outline-none transition placeholder:text-cream-400 focus:border-primary focus:ring-4 focus:ring-accent/20`;

export function TextInput({ label, required, error, hint, id, className = '', ...props }: InputHTMLAttributes<HTMLInputElement> & { label?: string; required?: boolean; error?: string; hint?: string }) {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const input = <input id={inputId} aria-invalid={Boolean(error)} className={`${inputClassName(error)} ${className}`} {...props} />;
  if (!label) {
    return input;
  }
  return <Field label={label} htmlFor={inputId} required={required} error={error} hint={hint}>{input}</Field>;
}

export function EmailInput(props: InputHTMLAttributes<HTMLInputElement> & { label?: string; required?: boolean; error?: string; hint?: string }) {
  return <TextInput type="email" inputMode="email" autoComplete="email" spellCheck={false} {...props} />;
}

export function PasswordInput(props: InputHTMLAttributes<HTMLInputElement> & { label?: string; required?: boolean; error?: string; hint?: string }) {
  return <TextInput type="password" autoComplete="current-password" {...props} />;
}

export function UrlInput(props: InputHTMLAttributes<HTMLInputElement> & { label?: string; required?: boolean; error?: string; hint?: string }) {
  return <TextInput type="url" inputMode="url" autoComplete="url" placeholder="https://" {...props} />;
}

export function DateInput(props: InputHTMLAttributes<HTMLInputElement> & { label?: string; required?: boolean; error?: string; hint?: string }) {
  return <TextInput type="date" {...props} />;
}

export function NumberInput(props: InputHTMLAttributes<HTMLInputElement> & { label?: string; required?: boolean; error?: string; hint?: string }) {
  return <TextInput type="number" inputMode="decimal" {...props} />;
}

export function PhoneInput({ label, required, error, hint, id, className = '', value, onChange, ...props }: InputHTMLAttributes<HTMLInputElement> & { label?: string; required?: boolean; error?: string; hint?: string }) {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const input = (
    <input
      id={inputId}
      aria-invalid={Boolean(error)}
      inputMode="tel"
      autoComplete="tel"
      value={value ?? ''}
      onChange={(event) => {
        event.target.value = formatPhoneInput(event.target.value);
        onChange?.(event);
      }}
      className={`${inputClassName(error)} ${className}`}
      {...props}
    />
  );
  if (!label) {
    return input;
  }
  return <Field label={label} htmlFor={inputId} required={required} error={error} hint={hint}>{input}</Field>;
}

export function SelectInput({ label, required, error, hint, children, className = '', ...props }: SelectHTMLAttributes<HTMLSelectElement> & { label?: string; required?: boolean; error?: string; hint?: string; children: ReactNode }) {
  const generatedId = useId();
  const inputId = props.id ?? generatedId;
  const select = (
    <select id={inputId} aria-invalid={Boolean(error)} className={`${inputClassName(error)} ${className}`} {...props}>
      {children}
    </select>
  );
  if (!label) {
    return select;
  }
  return <Field label={label} htmlFor={inputId} required={required} error={error} hint={hint}>{select}</Field>;
}

export function TextArea({ label, required, error, hint, rows = 4, className = '', ...props }: TextareaHTMLAttributes<HTMLTextAreaElement> & { label?: string; required?: boolean; error?: string; hint?: string }) {
  const generatedId = useId();
  const inputId = props.id ?? generatedId;
  const textarea = <textarea id={inputId} rows={rows} aria-invalid={Boolean(error)} className={`${inputClassName(error)} resize-y ${className}`} {...props} />;
  if (!label) {
    return textarea;
  }
  return <Field label={label} htmlFor={inputId} required={required} error={error} hint={hint}>{textarea}</Field>;
}
