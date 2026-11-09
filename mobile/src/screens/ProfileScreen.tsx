import { useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { ApiError, updateProfile } from '../api/client';
import { useAuth } from '../auth';
import { Badge, Button, Card, ErrorText, Field, Input } from '../components';
import { colors } from '../theme';

export function ProfileScreen() {
  const { owner, refreshProfile, logout } = useAuth();
  const [displayName, setDisplayName] = useState('');
  const [bio, setBio] = useState('');
  const [phone, setPhone] = useState('');
  const [avatarUrl, setAvatarUrl] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!owner) {
      return;
    }
    setDisplayName(owner.displayName);
    setBio(owner.bio ?? '');
    setPhone(owner.phone ?? '');
    setAvatarUrl(owner.avatarUrl ?? '');
  }, [owner]);

  async function submit() {
    setError(null);
    setSaved(false);
    setLoading(true);
    try {
      await updateProfile({ displayName, bio: bio || undefined, phone: phone || undefined, avatarUrl: avatarUrl || undefined });
      await refreshProfile();
      setSaved(true);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to update profile');
    } finally {
      setLoading(false);
    }
  }

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <Text style={styles.title}>Profile</Text>
      <Card>
        <Text style={styles.name}>{displayName || owner?.displayName}</Text>
        <Text style={styles.email}>{owner?.email}</Text>
        <View style={styles.badgeRow}><Badge label="Pet owner" tone="accent" /></View>
      </Card>
      <Card title="Account details">
        <Field label="Display name"><Input value={displayName} onChangeText={setDisplayName} /></Field>
        <Field label="Bio"><Input value={bio} onChangeText={setBio} multiline /></Field>
        <Field label="Phone"><Input value={phone} onChangeText={setPhone} keyboardType="phone-pad" /></Field>
        <Field label="Avatar URL"><Input value={avatarUrl} onChangeText={setAvatarUrl} autoCapitalize="none" /></Field>
        <ErrorText message={error} />
        {saved ? <Text style={styles.saved}>Profile updated.</Text> : null}
        <Button label={loading ? 'Saving…' : 'Save profile'} onPress={submit} disabled={loading} />
        <View style={styles.logout}><Button label="Logout" variant="secondary" onPress={logout} /></View>
      </Card>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.cream50 },
  content: { padding: 20, gap: 16 },
  title: { color: colors.cream900, fontSize: 30, fontWeight: '900' },
  name: { color: colors.cream900, fontSize: 22, fontWeight: '900', textAlign: 'center' },
  email: { color: colors.cream500, textAlign: 'center', marginTop: 4 },
  badgeRow: { alignItems: 'center', marginTop: 12 },
  saved: { color: colors.success, fontWeight: '700', marginBottom: 12 },
  logout: { marginTop: 10 },
});
