import { useEffect, useMemo, useState } from 'react';
import { ApiError, listPromotions } from '../api/client';
import { Badge, EmptyState, ErrorMessage, PawMark, SectionHeader } from '../components/ui';
import type { OwnerPromotion } from '../types';

const GRADIENTS = [
  'var(--color-teal-light)',
  'var(--color-mint)',
  'var(--color-sand-deep)',
  'var(--color-amber-soft)',
];

function websiteHref(website: string | null): string | null {
  const value = website?.trim();
  if (!value) return null;
  return /^https?:\/\//i.test(value) ? value : `https://${value}`;
}

function websiteLabel(website: string | null): string | null {
  const value = website?.trim();
  if (!value) return null;
  try {
    return new URL(/^https?:\/\//i.test(value) ? value : `https://${value}`).hostname.replace(/^www\./, '');
  } catch {
    return value;
  }
}

function expiryLabel(expiresAt: string | null): string {
  if (!expiresAt) return 'No expiry';
  return `Until ${new Date(expiresAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}`;
}

function PromotionCard({ promotion, index }: { promotion: OwnerPromotion; index: number }) {
  const href = websiteHref(promotion.partnerWebsite);
  const category = promotion.targetAudience.trim() || 'Pet-friendly';
  const location = websiteLabel(promotion.partnerWebsite);
  return (
    <article className="pl-card overflow-hidden">
      <div className="relative flex h-40 items-center justify-center" style={{ background: GRADIENTS[index % GRADIENTS.length] }}>
        <PawMark className="h-16 w-16 text-white/80" />
        <div className="absolute left-3 top-3 flex gap-2">
          <Badge tone="accent">Pet-friendly</Badge>
          <Badge tone="danger">Promo</Badge>
        </div>
        <div className="absolute bottom-3 left-3 rounded-full bg-white/90 px-3 py-1 text-label font-bold text-ink shadow-sm backdrop-blur">
          {category}
        </div>
      </div>
      <div className="p-5">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <p className="text-label font-bold uppercase tracking-[0.18em] text-muted">{promotion.partnerName}</p>
            <h3 className="mt-2 truncate font-display text-xl font-bold text-ink">{promotion.title}</h3>
          </div>
          <Badge tone="neutral">{expiryLabel(promotion.expiresAt)}</Badge>
        </div>
        <p className="mt-2 text-body leading-6 text-muted">{promotion.description}</p>
        <div className="mt-4 flex items-center justify-between gap-3">
          <div className="min-w-0 text-label font-semibold text-muted">
            <span className="block truncate">{location ?? 'Partner offer'}</span>
            <span className="mt-1 inline-block">{promotion.callToAction}</span>
          </div>
          {href ? (
            <a href={href} target="_blank" rel="noreferrer" className="inline-flex shrink-0 rounded-2xl bg-teal px-4 py-2 text-body font-bold text-white shadow-card hover:bg-teal-dark">
              Visit
            </a>
          ) : (
            <span className="inline-flex shrink-0 rounded-2xl bg-sand-deep px-4 py-2 text-body font-bold text-muted">{promotion.callToAction}</span>
          )}
        </div>
      </div>
    </article>
  );
}

export function BusinessesPage() {
  const [promotions, setPromotions] = useState<OwnerPromotion[]>([]);
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('All offers');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    listPromotions()
      .then((items) => { if (active) setPromotions(items); })
      .catch((caught) => { if (active) setError(caught instanceof ApiError ? caught.message : 'Unable to load businesses'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  const categories = useMemo(() => {
    const values = promotions.map((item) => item.targetAudience.trim()).filter(Boolean);
    return ['All offers', ...Array.from(new Set(values))];
  }, [promotions]);

  const filtered = useMemo(() => {
    const term = query.trim().toLowerCase();
    return promotions.filter((item) => {
      const matchesCategory = category === 'All offers' || item.targetAudience.trim() === category;
      const matchesQuery = !term || [item.partnerName, item.title, item.description, item.targetAudience]
        .some((value) => value.toLowerCase().includes(term));
      return matchesCategory && matchesQuery;
    });
  }, [promotions, query, category]);

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <SectionHeader
        title="Businesses"
        subtitle="Discover pet-friendly partners and active campaigns near your pack's world."
        action={<Badge tone="accent">{promotions.length} active offers</Badge>}
      />
      <div className="pl-card p-4">
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Search businesses, offers, or categories"
          className="w-full rounded-2xl border border-line bg-white px-4 py-3 text-body text-ink outline-none placeholder:text-muted focus:border-teal"
        />
        <div className="mt-3 flex gap-2 overflow-x-auto pb-1">
          {categories.map((item) => (
            <button
              key={item}
              type="button"
              onClick={() => setCategory(item)}
              className={`shrink-0 rounded-full px-4 py-2 text-body font-bold transition ${
                category === item
                  ? 'bg-teal text-white shadow-card'
                  : 'border border-line bg-white text-muted hover:border-amber hover:text-teal'
              }`}
            >
              {item}
            </button>
          ))}
        </div>
      </div>
      <ErrorMessage message={error} />
      {loading ? (
        <div className="pl-card p-8 text-body text-muted">Loading businesses…</div>
      ) : filtered.length === 0 ? (
        <EmptyState title="No promotions yet" description="Partner campaigns will appear here when they are active." />
      ) : (
        <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
          {filtered.map((promotion, index) => <PromotionCard key={promotion.campaignId} promotion={promotion} index={index} />)}
        </div>
      )}
    </div>
  );
}
