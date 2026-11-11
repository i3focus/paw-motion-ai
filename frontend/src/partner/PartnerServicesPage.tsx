import { useEffect, useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import { createPartnerService, listPartnerServices, updatePartnerService, type PartnerService } from '../api/partner';
import { Badge, Button, Card, EmptyState, ErrorMessage, LoadingState, NumberInput, SectionHeader, TextArea, TextInput } from '../components/ui';

const emptyForm = { id: '', name: '', description: '', durationMinutes: '60', price: '', active: true };

export function PartnerServicesPage() {
  const [services, setServices] = useState<PartnerService[] | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [editing, setEditing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const reload = () => listPartnerServices().then(setServices).catch((e) => { setError(e instanceof ApiError ? e.message : 'Unable to load services'); setServices([]); });
  useEffect(() => { reload(); }, []);

  function edit(s: PartnerService) {
    setForm({ id: s.id ?? '', name: s.name ?? '', description: s.description ?? '', durationMinutes: String(s.durationMinutes ?? 60), price: ((s.priceCents ?? 0) / 100).toString(), active: s.active ?? true });
    setEditing(true);
  }

  async function save(e: FormEvent) {
    e.preventDefault();
    const minutes = Number(form.durationMinutes);
    const price = Number(form.price);
    if (!form.name.trim()) { setError('Name is required'); return; }
    if (!Number.isFinite(minutes) || minutes < 5 || minutes > 1440) { setError('Duration must be between 5 and 1440 minutes'); return; }
    if (!Number.isFinite(price) || price < 0) { setError('Enter a valid price'); return; }
    setBusy(true);
    setError(null);
    try {
      const input = {
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        durationMinutes: minutes,
        priceCents: Math.round(price * 100),
        active: form.active,
      };
      if (form.id) await updatePartnerService(form.id, input);
      else await createPartnerService(input);
      setForm(emptyForm);
      setEditing(false);
      await reload();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to save service');
    } finally {
      setBusy(false);
    }
  }

  async function toggle(s: PartnerService) {
    if (!s.id) return;
    setError(null);
    try {
      await updatePartnerService(s.id, {
        name: s.name ?? null,
        description: s.description ?? undefined,
        durationMinutes: s.durationMinutes ?? null,
        priceCents: s.priceCents ?? null,
        active: !s.active,
      });
      await reload();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to update service');
    }
  }

  return (
    <div className="space-y-6">
      <SectionHeader title="Services" subtitle="Bookable services owners and PetLife AI can reserve — vet visits, grooming, walks, boarding." />
      <ErrorMessage message={error} />
      <div className="grid gap-6 lg:grid-cols-[400px_1fr]">
        <Card title={form.id ? 'Edit service' : 'New service'}>
          <form onSubmit={save} className="space-y-4" noValidate>
            <TextInput label="Name" required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} maxLength={120} placeholder="Full grooming session" />
            <TextArea label="Description" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} rows={3} maxLength={500} />
            <div className="grid gap-4 sm:grid-cols-2">
              <NumberInput label="Duration (min)" required min={5} max={1440} value={form.durationMinutes} onChange={(e) => setForm({ ...form, durationMinutes: e.target.value })} />
              <NumberInput label="Price" required min={0} step="0.01" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} hint="In your billing currency" />
            </div>
            <label className="flex items-center gap-2 text-body text-ink">
              <input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} className="h-4 w-4 accent-teal" />
              Available for booking
            </label>
            <div className="flex gap-2">
              <Button type="submit" disabled={busy || !form.name.trim()}>{busy ? 'Saving…' : form.id ? 'Save changes' : 'Add service'}</Button>
              {editing ? <Button type="button" variant="ghost" onClick={() => { setForm(emptyForm); setEditing(false); }}>Cancel</Button> : null}
            </div>
          </form>
        </Card>

        <Card title="Your services">
          {services === null ? <LoadingState /> : services.length === 0 ? (
            <EmptyState title="No services yet" description="Add a bookable service so campaigns can use a Book CTA and PetLife AI can schedule it. If saving fails, complete your business profile first." />
          ) : (
            <div className="space-y-3">
              {services.map((s) => (
                <div key={s.id} className="flex items-center gap-3 rounded-2xl border border-line bg-cream p-4">
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <h3 className="font-bold text-ink">{s.name}</h3>
                      <Badge tone={s.active ? 'success' : 'neutral'}>{s.active ? 'Active' : 'Paused'}</Badge>
                    </div>
                    <p className="text-label text-muted">{s.durationMinutes} min · {((s.priceCents ?? 0) / 100).toFixed(2)} {s.currency}{s.description ? ` · ${s.description}` : ''}</p>
                  </div>
                  <Button variant="secondary" onClick={() => edit(s)}>Edit</Button>
                  <Button variant="ghost" onClick={() => toggle(s)}>{s.active ? 'Pause' : 'Activate'}</Button>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
