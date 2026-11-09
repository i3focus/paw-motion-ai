import type { ReactNode } from 'react';

export function BrandLogo({ className = '' }: { className?: string }) {
  return (
    <span className={`font-display font-bold text-secondary ${className}`}>
      PawMotion <span className="text-accent">AI</span>
    </span>
  );
}

export function Card({ title, children, className = '' }: { title?: string; children: ReactNode; className?: string }) {
  return (
    <section className={`rounded-2xl border border-cream-200 bg-white p-5 shadow-[0_20px_50px_-30px_rgba(54,39,20,0.45)] ${className}`}>
      {title ? <h2 className="mb-4 text-lg font-semibold text-cream-900">{title}</h2> : null}
      {children}
    </section>
  );
}

export function StatCard({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-2xl border border-cream-200 bg-white p-4 shadow-[0_20px_50px_-30px_rgba(54,39,20,0.45)]">
      <p className="text-sm text-cream-500">{label}</p>
      <p className="mt-2 text-3xl font-bold text-cream-900">{value}</p>
    </div>
  );
}

export function ErrorMessage({ message }: { message: string | null }) {
  if (!message) {
    return null;
  }
  return <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">{message}</p>;
}

export function EmptyState({ title, description }: { title: string; description: string }) {
  return (
    <div className="rounded-2xl border border-dashed border-cream-300 bg-cream-50 p-6 text-center">
      <h3 className="font-semibold text-cream-900">{title}</h3>
      <p className="mt-1 text-sm text-cream-600">{description}</p>
    </div>
  );
}
