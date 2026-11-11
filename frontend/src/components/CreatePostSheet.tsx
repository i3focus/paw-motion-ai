import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createPost, uploadMedia, writeCaption } from '../api/petlife';
import type { Pet } from '../types';
import { BottomSheet, Button, ErrorMessage, Field, SelectInput, TextArea } from './ui';

export function CreatePostSheet({ open, onClose, pets }: { open: boolean; onClose: () => void; pets: Pet[] }) {
  const navigate = useNavigate();
  const fileRef = useRef<HTMLInputElement>(null);
  const [petId, setPetId] = useState('');
  const [caption, setCaption] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [placeName, setPlaceName] = useState('');
  const [busy, setBusy] = useState<'idle' | 'uploading' | 'posting' | 'ai'>('idle');
  const [error, setError] = useState<string | null>(null);

  const resolvedPetId = petId || pets[0]?.id || '';

  function pickFile(f: File | null) {
    setFile(f);
    if (preview) URL.revokeObjectURL(preview);
    setPreview(f ? URL.createObjectURL(f) : null);
  }

  async function aiCaption() {
    if (!resolvedPetId) return;
    setBusy('ai');
    setError(null);
    try {
      const r = await writeCaption(resolvedPetId, caption.trim() || undefined);
      if (r.caption) setCaption(r.caption);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'PetLife AI could not write a caption right now');
    } finally {
      setBusy('idle');
    }
  }

  async function publish() {
    if (!resolvedPetId || !file || busy !== 'idle') return;
    setError(null);
    try {
      setBusy('uploading');
      const media = await uploadMedia(file);
      setBusy('posting');
      await createPost({ petId: resolvedPetId, caption: caption.trim() || undefined, placeName: placeName.trim() || undefined, mediaIds: [media.id as string] });
      onClose();
      setCaption(''); setPlaceName(''); pickFile(null);
      navigate('/app/feed', { state: { refresh: Date.now() } });
      window.dispatchEvent(new CustomEvent('petlife:feed-refresh'));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not publish the post');
    } finally {
      setBusy('idle');
    }
  }

  const petName = pets.find((p) => p.id === resolvedPetId)?.name ?? pets[0]?.name ?? 'your pet';

  return (
    <BottomSheet open={open} title="New post" onClose={onClose}>
      <div className="space-y-4">
        <p className="text-label font-bold text-muted">Posting as <span className="text-teal">{petName}</span></p>
        {pets.length > 1 ? (
          <SelectInput label="Pet" value={resolvedPetId} onChange={(e) => setPetId(e.target.value)}>
            {pets.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
          </SelectInput>
        ) : null}

        <Field label="Caption" hint={`${caption.length}/200`}>
          <TextArea rows={3} maxLength={200} value={caption} onChange={(e) => setCaption(e.target.value)} placeholder="What did your pet do today?" />
        </Field>
        <button type="button" onClick={aiCaption} disabled={busy !== 'idle' || !resolvedPetId}
          className="inline-flex min-h-9 items-center gap-2 rounded-pill bg-teal-light px-3 text-label font-extrabold text-teal hover:bg-mint disabled:opacity-50">
          ✨ {busy === 'ai' ? 'Writing…' : 'Write with PetLife AI'}
        </button>

        <input type="text" value={placeName} onChange={(e) => setPlaceName(e.target.value)} placeholder="Add a place (optional)" aria-label="Place"
          className="min-h-12 w-full rounded-md border-2 border-line bg-white px-4 text-body font-semibold text-ink outline-none focus:border-teal" />

        <input ref={fileRef} type="file" accept="image/*" className="sr-only" aria-label="Choose a photo" onChange={(e) => pickFile(e.target.files?.[0] ?? null)} />
        {preview ? (
          <div className="relative">
            <img src={preview} alt="Selected" className="h-52 w-full rounded-lg object-cover" />
            <button type="button" onClick={() => pickFile(null)} aria-label="Remove photo" className="absolute right-2 top-2 grid h-9 w-9 place-items-center rounded-full bg-white font-black text-teal shadow-card">×</button>
          </div>
        ) : (
          <button type="button" onClick={() => fileRef.current?.click()} className="flex h-32 w-full items-center justify-center rounded-lg border-2 border-dashed border-line bg-sand text-label font-extrabold text-teal hover:bg-sand-deep">
            📷 Choose a photo
          </button>
        )}

        <ErrorMessage message={error} />
        <div className="flex gap-2 pt-1">
          <Button variant="ghost" onClick={onClose} className="flex-1">Cancel</Button>
          <Button onClick={publish} disabled={!resolvedPetId || !file || busy !== 'idle'} className="flex-1">
            {busy === 'uploading' ? 'Uploading…' : busy === 'posting' ? 'Publishing…' : 'Publish'}
          </Button>
        </div>
      </div>
    </BottomSheet>
  );
}
