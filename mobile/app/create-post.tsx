import { useEffect, useState } from 'react';
import { Image, KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useRouter } from 'expo-router';
import * as ImagePicker from 'expo-image-picker';
import { createPost, uploadMedia, writeCaption } from '../src/api/petlife';
import { listPets } from '../src/api/client';
import type { Pet } from '../src/types';
import { Button, ErrorText, Field, Input } from '../src/components';
import { colors, fonts, fontSize, radius } from '../src/theme';

export default function CreatePostScreen() {
  const router = useRouter();
  const [pets, setPets] = useState<Pet[]>([]);
  const [petId, setPetId] = useState('');
  const [caption, setCaption] = useState('');
  const [placeName, setPlaceName] = useState('');
  const [asset, setAsset] = useState<ImagePicker.ImagePickerAsset | null>(null);
  const [busy, setBusy] = useState<'idle' | 'uploading' | 'posting' | 'ai'>('idle');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => { listPets().then(setPets).catch(() => setPets([])); }, []);
  const resolvedPet = petId || pets[0]?.id || '';
  const petName = pets.find((p) => p.id === resolvedPet)?.name ?? pets[0]?.name ?? 'your pet';

  async function pick() {
    const perm = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!perm.granted) { setError('Photo library access is needed to pick a picture'); return; }
    const result = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], quality: 0.85, allowsMultipleSelection: false });
    if (!result.canceled) setAsset(result.assets[0] ?? null);
  }

  async function aiCaption() {
    if (!resolvedPet) return;
    setBusy('ai');
    setError(null);
    try {
      const r = await writeCaption(resolvedPet, caption.trim() || undefined);
      if (r.caption) setCaption(r.caption);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'PetLife AI could not write a caption right now');
    } finally {
      setBusy('idle');
    }
  }

  async function publish() {
    if (!resolvedPet || !asset || busy !== 'idle') return;
    setError(null);
    try {
      setBusy('uploading');
      const media = await uploadMedia(asset);
      if (!media.id) throw new Error('Upload did not return a media id');
      setBusy('posting');
      await createPost({ petId: resolvedPet, caption: caption.trim() || undefined, placeName: placeName.trim() || undefined, mediaIds: [media.id] });
      router.back();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not publish the post');
      setBusy('idle');
    }
  }

  return (
    <KeyboardAvoidingView style={s.screen} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={s.content} keyboardShouldPersistTaps="handled">
        <View style={s.head}>
          <Text style={s.title} accessibilityRole="header">New post</Text>
          <Pressable onPress={() => router.back()} accessibilityLabel="Cancel" style={s.x}><Text style={s.xText}>×</Text></Pressable>
        </View>
        <Text style={s.as}>Posting as <Text style={s.asName}>{petName}</Text></Text>

        {pets.length > 1 ? (
          <Field label="Pet">
            <View style={s.chips}>
              {pets.map((p) => (
                <Pressable key={p.id} onPress={() => setPetId(p.id)} style={[s.opt, resolvedPet === p.id && s.optOn]}>
                  <Text style={[s.optText, resolvedPet === p.id && s.optTextOn]}>{p.name}</Text>
                </Pressable>
              ))}
            </View>
          </Field>
        ) : null}

        <Field label={`Caption  ${caption.length}/200`}>
          <Input value={caption} onChangeText={(v) => setCaption(v.slice(0, 200))} placeholder="What did your pet do today?" multiline style={s.caption} />
        </Field>
        <Pressable onPress={aiCaption} disabled={busy !== 'idle' || !resolvedPet} accessibilityRole="button" style={s.aiChip}>
          <Text style={s.aiChipText}>✨ {busy === 'ai' ? 'Writing…' : 'Write with PetLife AI'}</Text>
        </Pressable>

        <Field label="Place (optional)"><Input value={placeName} onChangeText={setPlaceName} placeholder="Add a place" /></Field>

        {asset ? (
          <View>
            <Image source={{ uri: asset.uri }} style={s.preview} />
            <Pressable onPress={() => setAsset(null)} style={s.remove} accessibilityLabel="Remove photo"><Text style={s.removeText}>×</Text></Pressable>
          </View>
        ) : (
          <Pressable onPress={pick} style={s.picker} accessibilityRole="button" accessibilityLabel="Choose a photo">
            <Text style={s.pickerText}>📷 Choose a photo</Text>
          </Pressable>
        )}

        <ErrorText message={error} />
        <View style={s.actions}>
          <View style={s.flex1}><Button label="Cancel" variant="ghost" onPress={() => router.back()} /></View>
          <View style={s.flex1}>
            <Button label={busy === 'uploading' ? 'Uploading…' : busy === 'posting' ? 'Publishing…' : 'Publish'}
              disabled={!resolvedPet || !asset || busy !== 'idle'} onPress={publish} />
          </View>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const s = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.cream },
  content: { padding: 20, paddingBottom: 40 },
  head: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 },
  title: { color: colors.tealDark, fontSize: fontSize.title, fontFamily: fonts.w900 },
  x: { width: 40, height: 40, borderRadius: 20, backgroundColor: colors.white, alignItems: 'center', justifyContent: 'center' },
  xText: { fontSize: 22, color: colors.teal, fontFamily: fonts.w900, marginTop: -2 },
  as: { color: colors.muted, fontSize: fontSize.label, fontFamily: fonts.w700, marginBottom: 12 },
  asName: { color: colors.teal, fontFamily: fonts.w900 },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  opt: { borderRadius: radius.pill, paddingHorizontal: 16, paddingVertical: 10, backgroundColor: colors.white, borderWidth: 2, borderColor: colors.line },
  optOn: { borderColor: colors.teal, backgroundColor: colors.tealLight },
  optText: { color: colors.teal, fontFamily: fonts.w800, fontSize: fontSize.label },
  optTextOn: { color: colors.tealDark },
  caption: { minHeight: 90, textAlignVertical: 'top' },
  aiChip: { alignSelf: 'flex-start', backgroundColor: colors.tealLight, borderRadius: radius.pill, paddingHorizontal: 14, paddingVertical: 9, marginBottom: 14 },
  aiChipText: { color: colors.teal, fontSize: fontSize.label, fontFamily: fonts.w900 },
  preview: { width: '100%', height: 240, borderRadius: radius.md, backgroundColor: colors.sand },
  remove: { position: 'absolute', top: 8, right: 8, width: 34, height: 34, borderRadius: 17, backgroundColor: colors.white, alignItems: 'center', justifyContent: 'center' },
  removeText: { fontSize: 20, color: colors.teal, fontFamily: fonts.w900, marginTop: -2 },
  picker: { height: 130, borderRadius: radius.md, borderWidth: 2, borderStyle: 'dashed', borderColor: colors.line, backgroundColor: colors.sand, alignItems: 'center', justifyContent: 'center' },
  pickerText: { color: colors.teal, fontSize: fontSize.label, fontFamily: fonts.w900 },
  actions: { flexDirection: 'row', gap: 10, marginTop: 18 },
  flex1: { flex: 1 },
});
