import { useEffect, useMemo, useState } from 'react';
import { ApiError, listPromotions } from '../api/client';
import { Badge, EmptyState, ErrorMessage, PoppyMark, SectionHeader } from '../components/ui';
import type { OwnerPromotion } from '../types';

const GRADIENTS = [
  'linear-gradient(135deg, #825e31 0%, #d99c52 55%, #b07f43 100%)',
  'linear-gradient(135deg, #5e4424 0%, #b07f43 55%, #d99c52 100%)',
  'linear-gradient(135deg, #362714 0%, #825e31 58%, #d99c52 100%)',
  'linear-gradient(135deg, #825e31 0%, #b07f43 45%, #362714 100%)',
];

function websiteHref(website: string | null): string | null {
  const value = website?.trim();
  if (!value) return null;
  return /^https?:\/\//i.test(value) ? value : `https://${value}`;
}

function expiryLabel(expiresAt: string | null): string {
  if (!expiresAt) return 'No expiry';
  return `Until ${new Date(expiresAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}`;
}

function PromotionCard({ promotion, index }: { promotion: OwnerPromotion; index: number }) {
  const href = websiteHref(promotion.partnerWebsite);
  return (
    <article className="game-card overflow-hidden">
      <div className="relative flex h-36 items-center justify-center" style={{ background: GRADIENTS[index % GRADIENTS.length] }}>
        <PoppyMark className="h-16 w-16 text-white/80" />
        <div className="absolute left-3 top-3 flex gap-2">
          <Badge tone="accent">Pet-friendly</Badge>
          <Badge tone="danger">Promo</Badge>
        </div>
      </div>
      <div className="p-5">
        <p className="text-xs font-bold uppercase tracking-[0.18em] text-cream-500">{promotion.partnerName}</p>
        <h3 className="mt-2 font-display text-xl font-bold text-cream-900">{promotion.title}</h3>
        <p className="mt-2 text-sm leading-6 text-cream-600">{promotion.description}</p>
        <div className="mt-4 flex items-center justify-between gap-3">
          <span className="text-xs font-semibold text-cream-500">{expiryLabel(promotion.expiresAt)}</span>
          {href ? (
            <a href={href} target="_blank" rel="noreferrer" className="inline-flex rounded-2xl bg-primary px-4 py-2 text-sm font-bold text-white shadow-[0_14px_30px_-18px_rgba(130,94,49,1)] hover:bg-secondary">
              {promotion.callToAction}
            </a>
          ) : (
            <span className="inline-flex rounded-2xl bg-cream-200 px-4 py-2 text-sm font-bold text-cream-600">{promotion.callToAction}</span>
          )}
        </div>
      </div>
    </article>
  );
}

export function BusinessesPage() {
  const [promotions, setPromotions] = useState<OwnerPromotion[]>([]);
  const [query, setQuery] = useState('');
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

  const filtered = useMemo(() => {
    const term = query.trim().toLowerCase();
    if (!term) return promotions;
    return promotions.filter((item) => [item.partnerName, item.title, item.description, item.targetAudience]
      .some((value) => value.toLowerCase().includes(term)));
  }, [promotions, query]);

  return (
    <div className="space-y-6">
      <SectionHeader title="Businesses" subtitle="Discover pet-friendly partners and active campaigns near your pack's world." />
      <div className="game-card p-4">
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Search businesses, offers, or categories"
          className="w-full rounded-2xl border border-cream-200 bg-white px-4 py-3 text-sm text-cream-900 outline-none placeholder:text-cream-400 focus:border-primary"
        />
      </div>
      <ErrorMessage message={error} />
      {loading ? (
        <div className="game-card p-8 text-sm text-cream-500">Loading businesses…</div>
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
