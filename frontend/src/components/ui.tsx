import { useEffect, useId, useRef, type ButtonHTMLAttributes, type InputHTMLAttributes, type ReactNode, type SVGProps, type SelectHTMLAttributes, type TextareaHTMLAttributes } from 'react';
import { reactions, type ReactionType } from '@petlife/design-tokens';
import { formatPhoneInput } from '../lib/validation';

export function PawMark({ className = '', ...props }: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 32 32" className={className} aria-hidden="true" {...props}>
      <rect width="32" height="32" rx="10" fill="var(--color-teal)" />
      <g fill="var(--color-cream)">
        <ellipse cx="9.5" cy="14" rx="2.5" ry="3.2" />
        <ellipse cx="14.5" cy="9.5" rx="2.5" ry="3.4" />
        <ellipse cx="21" cy="9.5" rx="2.5" ry="3.4" />
        <ellipse cx="25" cy="14.5" rx="2.4" ry="3.2" transform="rotate(10 25 14.5)" />
        <path d="M17.5 14c3.6 0 7 4 7 7.3 0 2.4-2 3.2-4.2 2.9-1.1-.2-1.8-.6-2.8-.6s-1.7.4-2.8.6c-2.2.4-4.2-.5-4.2-2.9 0-3.3 3.4-7.3 7-7.3z" />
      </g>
    </svg>
  );
}

export function BrandLogo({ compact = false, className = '' }: { compact?: boolean; className?: string }) {
  return (
    <span className={`inline-flex items-center gap-2 font-black tracking-[0.02em] text-teal ${className || 'text-heading'}`}>
      <PawMark className="h-[1.2em] w-[1.2em]" />
      {!compact ? <span>PETLIFE</span> : <span className="sr-only">PETLIFE</span>}
    </span>
  );
}

export function Avatar({ name, imageUrl, src, size = 'md', className = '' }: { name: string; imageUrl?: string | null; src?: string | null; size?: 'sm' | 'md' | 'lg'; className?: string }) {
  const initials = name.split(' ').filter(Boolean).slice(0, 2).map((part) => part[0]).join('').toUpperCase();
  const source = imageUrl ?? src;
  const sizeClass = size === 'lg' ? 'h-16 w-16 text-title' : size === 'sm' ? 'h-9 w-9 text-caption' : 'h-11 w-11 text-label';
  const dimensionClass = className || sizeClass;
  if (source) {
    return <img src={source} alt={name} className={`${dimensionClass} rounded-full bg-sand-deep object-cover`} />;
  }
  return <span className={`${dimensionClass} inline-flex items-center justify-center rounded-full bg-teal-light font-extrabold text-teal`} aria-label={name}>{initials || 'PL'}</span>;
}

export function StoryRing({ state = 'seen', children, label }: { state?: 'new' | 'unseen' | 'seen' | 'add'; children: ReactNode; label: string }) {
  const ring = state === 'seen' || state === 'add'
    ? 'bg-sand-deep'
    : 'bg-[conic-gradient(var(--color-teal),var(--color-amber),var(--color-teal))]';
  const glow = state === 'new' ? 'shadow-[0_0_0_4px_rgba(158,216,204,0.55),0_0_18px_6px_rgba(158,216,204,0.9)]' : '';
  return (
    <span className="inline-flex w-[72px] flex-col items-center gap-1 text-center text-label font-bold text-ink">
      <span className={`relative grid h-[72px] w-[72px] place-items-center rounded-full p-[3px] ${ring} ${glow}`}>
        <span className="h-full w-full overflow-hidden rounded-full border-[3px] border-cream">{children}</span>
        {state === 'add' ? <span className="absolute -bottom-0.5 -right-0.5 grid h-[26px] w-[26px] place-items-center rounded-full border-[3px] border-cream bg-teal text-[18px] font-black leading-none text-white" aria-hidden="true">+</span> : null}
      </span>
      <span className="max-w-full truncate">{label}</span>
    </span>
  );
}

type BadgeTone = 'neutral' | 'success' | 'warning' | 'danger' | 'accent' | 'sponsored';

export function Badge({ children, tone = 'neutral' }: { children: ReactNode; tone?: BadgeTone }) {
  const tones: Record<BadgeTone, string> = {
    neutral: 'bg-sand text-ink',
    success: 'bg-success-soft text-success',
    warning: 'bg-amber-soft text-amber-ink',
    danger: 'bg-danger-soft text-danger',
    accent: 'bg-teal-light text-teal',
    sponsored: 'border border-amber bg-amber-soft text-amber-ink',
  };
  return <span className={`inline-flex items-center gap-1 rounded-pill px-2.5 py-1 text-caption font-extrabold ${tones[tone]}`}>{children}</span>;
}

export function SponsoredLabel() {
  return <Badge tone="sponsored">Sponsored</Badge>;
}

export function Card({ children, className = '', title, description, variant = 'white' }: { children: ReactNode; className?: string; title?: string; description?: string; variant?: 'white' | 'sand' | 'sponsored' }) {
  const base = variant === 'sand' ? 'pl-card-sand' : variant === 'sponsored' ? 'pl-card ring-2 ring-amber ring-inset' : 'pl-card';
  return (
    <section className={`${base} ${className}`}>
      {title ? (
        <div className="px-5 pt-5 sm:px-6">
          <h2 className="text-title">{title}</h2>
          {description ? <p className="mt-1 text-body text-muted">{description}</p> : null}
        </div>
      ) : null}
      <div className={title ? 'p-5 sm:p-6' : ''}>{children}</div>
    </section>
  );
}

export function StatCard({ label, value, hint }: { label: string; value: ReactNode; hint?: string }) {
  return (
    <Card className="p-5">
      <p className="text-label font-bold text-muted">{label}</p>
      <p className="mt-2 text-display font-black text-teal-dark">{value}</p>
      {hint ? <p className="mt-1 text-label text-muted">{hint}</p> : null}
    </Card>
  );
}

export function SectionHeader({ title, description, subtitle, action }: { title: string; description?: string; subtitle?: string; action?: ReactNode }) {
  const text = description ?? subtitle;
  return (
    <div className="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h2 className="text-heading">{title}</h2>
        {text ? <p className="mt-1 text-body text-muted">{text}</p> : null}
      </div>
      {action}
    </div>
  );
}

export function ListRow({ leading, title, subtitle, trailing }: { leading?: ReactNode; title: ReactNode; subtitle?: ReactNode; trailing?: ReactNode }) {
  return (
    <div className="flex items-center gap-3 rounded-lg bg-sand p-3">
      {leading}
      <div className="min-w-0 flex-1">
        <p className="truncate font-extrabold text-ink">{title}</p>
        {subtitle ? <p className="truncate text-label text-muted">{subtitle}</p> : null}
      </div>
      {trailing}
    </div>
  );
}

export function ErrorMessage({ message }: { message?: string | null }) {
  if (!message) {
    return null;
  }
  return <p role="alert" className="rounded-md bg-danger-soft px-4 py-3 text-body font-bold text-danger">{message}</p>;
}

export function EmptyState({ title, description, action }: { title: string; description: string; action?: ReactNode }) {
  return (
    <Card variant="sand" className="flex flex-col items-center justify-center p-8 text-center">
      <PawMark className="h-14 w-14" />
      <h3 className="mt-4 text-title">{title}</h3>
      <p className="mt-2 max-w-md text-body text-muted">{description}</p>
      {action ? <div className="mt-5">{action}</div> : null}
    </Card>
  );
}

export function LoadingState({ label = 'Loading…' }: { label?: string }) {
  return (
    <div role="status" className="flex items-center justify-center gap-3 p-8 text-body text-muted">
      <span className="h-5 w-5 animate-spin rounded-full border-[3px] border-teal-light border-t-teal" aria-hidden="true" />
      {label}
    </div>
  );
}

type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger';
type ButtonSize = 'sm' | 'md' | 'lg';

export function Button({ children, variant = 'primary', size = 'md', className = '', ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: ButtonVariant; size?: ButtonSize }) {
  const variants: Record<ButtonVariant, string> = {
    primary: 'bg-coral text-coral-ink shadow-coral hover:-translate-y-0.5',
    secondary: 'bg-teal text-white hover:bg-teal-dark',
    outline: 'border-2 border-teal bg-transparent text-teal hover:bg-teal hover:text-white',
    ghost: 'text-teal hover:bg-teal-light',
    danger: 'bg-danger-soft text-danger hover:bg-coral-soft',
  };
  const sizes: Record<ButtonSize, string> = {
    sm: 'min-h-11 px-4 text-body',
    md: 'min-h-12 px-5 text-body',
    lg: 'min-h-[52px] px-6 text-body-lg',
  };
  return <button className={`inline-flex items-center justify-center gap-2 rounded-pill font-black transition active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50 disabled:shadow-none disabled:hover:translate-y-0 ${sizes[size]} ${variants[variant]} ${className}`} {...props}>{children}</button>;
}

export function IconButton({ label, children, className = '', ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { label: string }) {
  return (
    <button type="button" aria-label={label} className={`relative grid h-11 w-11 place-items-center rounded-full bg-white text-teal shadow-card transition hover:bg-teal-light ${className}`} {...props}>
      {children}
    </button>
  );
}

export function Chip({ selected = false, children, className = '', ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { selected?: boolean }) {
  return (
    <button type="button" aria-pressed={selected} className={`inline-flex min-h-11 shrink-0 items-center gap-2 rounded-pill px-4 text-label font-extrabold transition ${selected ? 'bg-teal text-white' : 'bg-white text-teal shadow-card hover:bg-teal-light'} ${className}`} {...props}>
      {children}
    </button>
  );
}

export function ReactionBar({ counts, selected, onReact }: { counts: Partial<Record<ReactionType, number>>; selected?: ReactionType | null; onReact: (type: ReactionType) => void }) {
  return (
    <div className="flex flex-wrap gap-2" role="group" aria-label="Reactions">
      {reactions.map((reaction) => (
        <button
          key={reaction.type}
          type="button"
          aria-pressed={selected === reaction.type}
          aria-label={`${reaction.label} (${counts[reaction.type] ?? 0})`}
          onClick={() => onReact(reaction.type)}
          className={`inline-flex min-h-11 items-center gap-1.5 rounded-pill px-3 text-label font-extrabold transition ${selected === reaction.type ? 'bg-teal-light text-teal-dark ring-2 ring-teal' : 'bg-sand text-ink hover:bg-teal-light'}`}
        >
          <span aria-hidden="true">{reaction.emoji}</span>
          {counts[reaction.type] ?? 0}
        </button>
      ))}
    </div>
  );
}

export function ProgressBar({ value, max = 100, label }: { value: number; max?: number; label: string }) {
  const percent = max > 0 ? Math.min(100, Math.max(0, (value / max) * 100)) : 0;
  return (
    <div className="pl-progress" role="progressbar" aria-label={label} aria-valuenow={value} aria-valuemin={0} aria-valuemax={max}>
      <div className="pl-progress-fill" style={{ width: `${percent}%` }} />
    </div>
  );
}

export function BottomSheet({ open, title, onClose, children }: { open: boolean; title: string; onClose: () => void; children: ReactNode }) {
  const titleId = useId();
  const dialogRef = useRef<HTMLDivElement>(null);
  const onCloseRef = useRef(onClose);
  onCloseRef.current = onClose;

  useEffect(() => {
    if (!open) return;
    const opener = document.activeElement as HTMLElement | null;
    dialogRef.current?.focus();
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onCloseRef.current();
    };
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      opener?.focus();
    };
  }, [open]);

  if (!open) {
    return null;
  }
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-overlay" onClick={onClose}>
      <div
        ref={dialogRef}
        tabIndex={-1}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className="pl-fade-in w-full max-w-[430px] rounded-t-sheet bg-cream p-5 pb-8 shadow-raised"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="mx-auto mb-4 h-1.5 w-12 rounded-pill bg-sand-deep" aria-hidden="true" />
        <div className="mb-4 flex items-center justify-between gap-3">
          <h2 id={titleId} className="text-title">{title}</h2>
          <IconButton label="Close" onClick={onClose}>
            <span aria-hidden="true" className="text-title leading-none">×</span>
          </IconButton>
        </div>
        {children}
      </div>
    </div>
  );
}

export type TabItem<K extends string> = { key: K; label: string; icon: ReactNode };

export function BottomTabBar<K extends string>({ items, active, onSelect, onCreate, createLabel = 'Create post' }: { items: [TabItem<K>, TabItem<K>, TabItem<K>, TabItem<K>]; active: K; onSelect: (key: K) => void; onCreate: () => void; createLabel?: string }) {
  const renderTab = (item: TabItem<K>) => (
    <button
      key={item.key}
      type="button"
      onClick={() => onSelect(item.key)}
      aria-current={active === item.key ? 'page' : undefined}
      className={`flex min-h-11 flex-1 flex-col items-center justify-center gap-0.5 text-caption font-extrabold ${active === item.key ? 'text-teal' : 'text-muted'}`}
    >
      <span className="h-6 w-6" aria-hidden="true">{item.icon}</span>
      {item.label}
    </button>
  );
  return (
    <nav aria-label="Main" className="flex h-[72px] items-center border-t border-line bg-white px-2">
      {items.slice(0, 2).map(renderTab)}
      <div className="flex flex-1 justify-center">
        <button type="button" aria-label={createLabel} onClick={onCreate} className="-mt-7 grid h-[60px] w-[60px] place-items-center rounded-full border-4 border-cream bg-coral text-[30px] font-black leading-none text-coral-ink shadow-coral">
          +
        </button>
      </div>
      {items.slice(2).map(renderTab)}
    </nav>
  );
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
      <label htmlFor={id} className="block text-label font-extrabold text-ink">
        {label}
        {required ? <span className="ml-1 text-danger" aria-hidden="true">*</span> : null}
      </label>
      {children}
      {error ? <p className="text-label font-bold text-danger">{error}</p> : hint ? <p className="text-label text-muted">{hint}</p> : null}
    </div>
  );
}

const inputClassName = (error?: string) => `w-full min-h-12 rounded-md border-2 ${error ? 'border-danger bg-danger-soft' : 'border-line bg-white'} px-4 py-3 text-body font-semibold text-ink outline-none transition placeholder:text-muted focus:border-teal`;

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
