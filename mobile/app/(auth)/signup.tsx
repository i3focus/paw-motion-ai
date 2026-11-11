import { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Link } from 'expo-router';
import { useAuth } from '../../src/auth';
import { Button, ErrorText, Field, Input } from '../../src/components';
import { colors, fonts, fontSize } from '../../src/theme';
import { isValidEmail, normalizeEmail } from '../../src/validation';
import { PawMark } from '../../src/petVisuals';
import { authStyles as styles } from './login';

export default function SignupScreen() {
  const { register } = useAuth();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit() {
    const normalized = normalizeEmail(email);
    if (name.trim().length < 2) { setError('Enter your name'); return; }
    if (!isValidEmail(normalized)) { setError('Enter a valid email address'); return; }
    if (password.length < 8) { setError('Password must be at least 8 characters'); return; }
    setBusy(true);
    setError(null);
    try {
      await register(normalized, password, name.trim());
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Sign up failed');
    } finally {
      setBusy(false);
    }
  }

  return (
    <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
        <View style={styles.logoRow}><PawMark size={34} /><Text style={styles.logo}>PETLIFE</Text></View>
        <Text style={styles.title} accessibilityRole="header">Create your account</Text>
        <Text style={styles.sub}>Your pet's profile is a minute away.</Text>
        <View style={styles.form}>
          <Field label="Your name"><Input value={name} onChangeText={setName} autoComplete="name" /></Field>
          <Field label="Email"><Input value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" autoComplete="email" /></Field>
          <Field label="Password"><Input value={password} onChangeText={setPassword} secureTextEntry autoComplete="new-password" /></Field>
          <Text style={s.hint}>At least 8 characters</Text>
          <ErrorText message={error} />
          <Button label={busy ? 'Please wait…' : 'Create account'} onPress={submit} disabled={busy} />
        </View>
        <Link href="/(auth)/login" style={styles.link}>Already have an account? <Text style={styles.linkBold}>Sign in</Text></Link>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const s = StyleSheet.create({
  hint: { color: colors.muted, fontSize: fontSize.caption, fontFamily: fonts.w600, marginTop: -6, marginBottom: 8 },
});
