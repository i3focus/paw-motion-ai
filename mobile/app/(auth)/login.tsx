import { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Link } from 'expo-router';
import { useAuth } from '../../src/auth';
import { Button, ErrorText, Field, Input } from '../../src/components';
import { colors, fonts, fontSize, radius } from '../../src/theme';
import { isValidEmail, normalizeEmail } from '../../src/validation';
import { PawMark } from '../../src/petVisuals';

export default function LoginScreen() {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit() {
    const normalized = normalizeEmail(email);
    if (!isValidEmail(normalized)) { setError('Enter a valid email address'); return; }
    if (password.length < 8) { setError('Password must be at least 8 characters'); return; }
    setBusy(true);
    setError(null);
    try {
      await login(normalized, password);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Sign in failed');
    } finally {
      setBusy(false);
    }
  }

  return (
    <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
        <View style={styles.logoRow}><PawMark size={34} /><Text style={styles.logo}>PETLIFE</Text></View>
        <Text style={styles.title} accessibilityRole="header">Welcome back</Text>
        <Text style={styles.sub}>Sign in to keep up with your pack.</Text>
        <View style={styles.form}>
          <Field label="Email"><Input value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" autoComplete="email" /></Field>
          <Field label="Password"><Input value={password} onChangeText={setPassword} secureTextEntry autoComplete="current-password" /></Field>
          <ErrorText message={error} />
          <Button label={busy ? 'Please wait…' : 'Sign in'} onPress={submit} disabled={busy} />
        </View>
        <Link href="/(auth)/signup" style={styles.link}>New to PetLife? <Text style={styles.linkBold}>Create a free profile</Text></Link>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

export const authStyles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.cream },
  container: { flexGrow: 1, justifyContent: 'center', padding: 24 },
  logoRow: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 28 },
  logo: { color: colors.teal, fontSize: fontSize.title, fontFamily: fonts.w900, letterSpacing: 1 },
  title: { color: colors.tealDark, fontSize: fontSize.display, fontFamily: fonts.w900 },
  sub: { color: colors.muted, fontSize: fontSize.body, fontFamily: fonts.w600, marginTop: 6, marginBottom: 20 },
  form: { gap: 4 },
  link: { color: colors.muted, fontSize: fontSize.body, fontFamily: fonts.w600, textAlign: 'center', marginTop: 24 },
  linkBold: { color: colors.teal, fontFamily: fonts.w900 },
});

const styles = authStyles;
